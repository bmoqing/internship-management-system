/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.entity.Attendance;
import fun.bmoqing.internship.entity.Company;
import fun.bmoqing.internship.dto.CheckinRequest;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.AttendanceMapper;
import fun.bmoqing.internship.mapper.CompanyMapper;
import fun.bmoqing.internship.mapper.AgreementMapper;
import fun.bmoqing.internship.service.InternshipRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;
    
    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private AgreementMapper agreementMapper;

    @Autowired
    private InternshipRecordService internshipRecordService;

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // Radius of the earth in m
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in m
    }

    @PostMapping("/checkin")
    public Result<?> checkin(@RequestBody CheckinRequest request) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以签到");
        }

        Long studentId = AuthUtil.currentUserId();
        Assignment activeAssignment = assignmentMapper.selectActiveByStudentId(studentId);
        if (activeAssignment == null) {
            return Result.validationError("当前无进行中实习分配，暂不能签到");
        }

        if (agreementMapper.countApprovedByAssignmentId(activeAssignment.getId()) == 0) {
            return Result.validationError("未完成三方协议签署及审核，无法进行实习活动，请先上传协议");
        }

        LocalDateTime now = LocalDateTime.now();
        if (activeAssignment.getAssignTime() != null && now.isBefore(activeAssignment.getAssignTime())) {
            return Result.validationError("实习尚未开始，暂不能签到");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        QueryWrapper<Attendance> query = new QueryWrapper<>();
        query.eq("student_id", studentId);
        query.ge("checkin_time", start);
        query.lt("checkin_time", end);
        if (attendanceMapper.selectCount(query) > 0) {
            return Result.error("今日已签到，请勿重复打卡");
        }
        
        Company company = null;
        if (activeAssignment.getCompanyId() != null) {
            company = companyMapper.selectById(activeAssignment.getCompanyId());
        }

        LocalTime nowTime = now.toLocalTime();
        String status = "NORMAL";
        
        if (company != null) {
            if (company.getCheckinStartTime() != null && company.getCheckinEndTime() != null) {
                if (nowTime.isBefore(company.getCheckinStartTime()) || nowTime.isAfter(company.getCheckinEndTime())) {
                    return Result.error("当前时间不在允许的签到时间段内");
                }
            }
            
            if (company.getWorkStartTime() != null) {
                if (nowTime.isAfter(company.getWorkStartTime())) {
                    status = "LATE";
                }
            } else if (nowTime.isAfter(LocalTime.of(9, 0))) {
                status = "LATE";
            }
            
            if (company.getLatitude() != null && company.getLongitude() != null) {
                if (request.getLatitude() == null || request.getLongitude() == null) {
                    return Result.error("签到需要获取当前地理位置");
                }
                double distance = calculateDistance(
                        company.getLatitude().doubleValue(), company.getLongitude().doubleValue(),
                        request.getLatitude().doubleValue(), request.getLongitude().doubleValue()
                );
                int radius = company.getRadius() != null ? company.getRadius() : 500;
                if (distance > radius) {
                    return Result.error("不在企业设定的考勤范围内(距离：" + (int)distance + "米，要求半径" + radius + "米)");
                }
            }
        } else {
            status = nowTime.isAfter(LocalTime.of(9, 0)) ? "LATE" : "NORMAL";
        }

        Attendance attendance = new Attendance();
        attendance.setStudentId(studentId);
        attendance.setCheckinTime(now);
        attendance.setStatus(status);
        attendanceMapper.insert(attendance);

        internshipRecordService.addRecord(
                studentId,
                "ATTENDANCE_CHECKIN",
                "签到成功（" + ("NORMAL".equals(status) ? "正常" : "迟到") + "）",
                attendance.getId()
        );

        return Result.success(attendance);
    }

    @GetMapping("/my")
    public Result<?> findMyAttendance(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以查看个人考勤");
        }

        Page<Attendance> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Attendance> query = new QueryWrapper<>();
        query.eq("student_id", AuthUtil.currentUserId());
        query.orderByDesc("checkin_time");
        return Result.success(attendanceMapper.selectPage(page, query));
    }

    @GetMapping("/list")
    public Result<?> findList(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师/管理员可查看考勤列表");
        }

        Page<Attendance> page = new Page<>(pageNum, pageSize);
        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(attendanceMapper.selectWithStudent(page, keyword));
        }
        return Result.success(attendanceMapper.selectWithStudentByTeacher(page, keyword, AuthUtil.currentUserId()));
    }

    @GetMapping("/my/stats")
    public Result<?> myStats() {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以查看个人考勤统计");
        }

        Long studentId = AuthUtil.currentUserId();
        Assignment activeAssignment = assignmentMapper.selectActiveByStudentId(studentId);

        long total = 0;
        long normal = 0;
        long late = 0;
        LocalDateTime assignStart = null;
        boolean hasActiveAssignment = activeAssignment != null;

        if (activeAssignment != null) {
            assignStart = activeAssignment.getAssignTime();
            total = countByStudentAndStatus(studentId, null, assignStart);
            normal = countByStudentAndStatus(studentId, "NORMAL", assignStart);
            late = countByStudentAndStatus(studentId, "LATE", assignStart);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("hasActiveAssignment", hasActiveAssignment);
        map.put("assignmentId", activeAssignment == null ? null : activeAssignment.getId());
        map.put("assignmentStart", assignStart);
        map.put("total", total);
        map.put("normal", normal);
        map.put("late", late);
        map.put("attendanceScore", calculateAttendanceScore(total, normal, late));
        
        if (hasActiveAssignment && activeAssignment.getCompanyId() != null) {
            Company company = companyMapper.selectById(activeAssignment.getCompanyId());
            if (company != null && company.getLatitude() != null && company.getLongitude() != null) {
                Map<String, Object> companyMapConfig = new HashMap<>();
                companyMapConfig.put("latitude", company.getLatitude());
                companyMapConfig.put("longitude", company.getLongitude());
                companyMapConfig.put("radius", company.getRadius() != null ? company.getRadius() : 500);
                companyMapConfig.put("checkinStartTime", company.getCheckinStartTime());
                companyMapConfig.put("checkinEndTime", company.getCheckinEndTime());
                companyMapConfig.put("workStartTime", company.getWorkStartTime());
                map.put("companyConfig", companyMapConfig);
            }
        }
        
        return Result.success(map);
    }

    private long countByStudentAndStatus(Long studentId, String status, LocalDateTime startTime) {
        QueryWrapper<Attendance> query = new QueryWrapper<>();
        query.eq("student_id", studentId);
        if (startTime != null) {
            query.ge("checkin_time", startTime);
        }
        if (status != null) {
            query.eq("status", status);
        }
        return attendanceMapper.selectCount(query);
    }

    private double calculateAttendanceScore(long total, long normal, long late) {
        if (total <= 0) {
            return 0D;
        }
        double score = (normal * 100.0 + late * 80.0) / total;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
