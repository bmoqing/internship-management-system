/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Application;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.entity.Attendance;
import fun.bmoqing.internship.entity.Log;
import fun.bmoqing.internship.entity.Report;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.AgreementMapper;
import fun.bmoqing.internship.mapper.AppealMapper;
import fun.bmoqing.internship.mapper.AttendanceMapper;
import fun.bmoqing.internship.mapper.ApplicationMapper;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.CompanyMapper;
import fun.bmoqing.internship.mapper.IncidentMapper;
import fun.bmoqing.internship.mapper.LogMapper;
import fun.bmoqing.internship.mapper.NoticeMapper;
import fun.bmoqing.internship.mapper.PositionMapper;
import fun.bmoqing.internship.mapper.ReportMapper;
import fun.bmoqing.internship.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final Logger log = LoggerFactory.getLogger(StatsController.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private AgreementMapper agreementMapper;

    @Autowired
    private AppealMapper appealMapper;

    @Autowired
    private IncidentMapper incidentMapper;

    @GetMapping
    public Result<?> getDashboardStats() {
        User currentUser = AuthUtil.currentUser();
        if (currentUser == null || !StringUtils.hasText(currentUser.getRole())) {
            return Result.unauthorized("请先登录");
        }

        String role = currentUser.getRole().toUpperCase();
        if ("ADMIN".equals(role)) {
            return Result.success(buildAdminStats());// 分发教务管理员全局透视数据
        }
        if ("TEACHER".equals(role)) {
            return Result.success(buildTeacherStats(currentUser)); // 分发教师管辖学生透视数据
        }
        if ("COMPANY".equals(role)) {
            if (currentUser.getCompanyId() == null) {
                return Result.validationError("企业账号未绑定企业，无法查看统计");// 分发企业关联岗位与在岗学生数据
            }
            return Result.success(buildCompanyStats(currentUser));
        }
        return Result.success(buildStudentStats(currentUser));// 分发学生个人实习记录与轨迹数据
    }

    private Map<String, Object> buildAdminStats() {
        Map<String, Object> map = new HashMap<>();

        map.put("userCount", userMapper.selectCount(null));
        map.put("companyCount", companyMapper.selectCount(null));
        map.put("positionCount", positionMapper.selectCount(null));
        map.put("appCount", applicationMapper.selectCount(null));
        map.put("assignmentCount", assignmentMapper.selectCount(null));
        map.put("logCount", logMapper.selectCount(null));
        map.put("reportCount", reportMapper.selectCount(null));
        map.put("attendanceCount", attendanceMapper.selectCount(null));

        map.put("userRoleChart", userMapper.selectCountByRole());
        map.put("applicationStatusChart", applicationMapper.selectCountByStatus());
        map.put("topPositionChart", applicationMapper.selectTopPositions(7));

        List<Map<String, Object>> applyTrendRaw = applicationMapper.selectApplyTrendLast7Days();
        List<Map<String, Object>> checkinTrendRaw = attendanceMapper.selectCheckinTrendLast7Days();
        List<Map<String, Object>> logTrendRaw = logMapper.selectLogTrendLast7Days();

        Map<String, Long> applyTrendMap = toTrendMap(applyTrendRaw);
        Map<String, Long> checkinTrendMap = toTrendMap(checkinTrendRaw);
        Map<String, Long> logTrendMap = toTrendMap(logTrendRaw);

        List<String> dates = last7Dates();
        List<Long> applyTrend = new ArrayList<>();
        List<Long> checkinTrend = new ArrayList<>();
        List<Long> logTrend = new ArrayList<>();

        for (String date : dates) {
            applyTrend.add(applyTrendMap.getOrDefault(date, 0L));
            checkinTrend.add(checkinTrendMap.getOrDefault(date, 0L));
            logTrend.add(logTrendMap.getOrDefault(date, 0L));
        }

        map.put("trendDates", dates);
        map.put("applyTrend", applyTrend);
        map.put("checkinTrend", checkinTrend);
        map.put("logTrend", logTrend);
        map.put("noticeList", noticeMapper.selectLatestActive(5));
        map.put("todoCards", buildAdminTodoCards());

        return map;
    }

    private Map<String, Object> buildTeacherStats(User currentUser) {
        Map<String, Object> map = baseScopedStatsTemplate();
        List<Long> studentIds = assignmentMapper.selectStudentIdsByTeacherId(currentUser.getId());

        map.put("userCount", safeCount("teacher.userCount", () -> userMapper.countStudentsByTeacherId(currentUser.getId())));
        map.put("companyCount", safeCount("teacher.companyCount", () -> assignmentMapper.countDistinctCompanyByTeacherId(currentUser.getId())));
        map.put("positionCount", safeCount("teacher.positionCount", () -> assignmentMapper.countDistinctPositionByTeacherId(currentUser.getId())));

        map.put("appCount", safeCount("teacher.appCount", () -> applicationMapper.countByReviewTeacherId(currentUser.getId())));
        map.put("assignmentCount", safeCount("teacher.assignmentCount", () -> countAssignmentByTeacher(currentUser.getId())));
        map.put("logCount", countLogByStudentIds(studentIds));
        map.put("reportCount", countReportByStudentIds(studentIds));
        map.put("attendanceCount", countAttendanceByStudentIds(studentIds));
        map.put("noticeList", noticeMapper.selectLatestActive(5));
        map.put("todoCards", buildTeacherTodoCards(currentUser));
        return map;
    }

    private Map<String, Object> buildCompanyStats(User currentUser) {
        Map<String, Object> map = baseScopedStatsTemplate();
        Long companyId = currentUser.getCompanyId();
        List<Long> studentIds = assignmentMapper.selectStudentIdsByCompanyId(companyId);

        QueryWrapper<Assignment> assignmentQuery = new QueryWrapper<>();
        assignmentQuery.eq("company_id", companyId).eq("status", 1);

        QueryWrapper<fun.bmoqing.internship.entity.Position> positionQuery = new QueryWrapper<>();
        positionQuery.eq("company_id", companyId);

        map.put("userCount", studentIds.size());
        map.put("companyCount", 1);
        map.put("positionCount", safeCount("company.positionCount", () -> positionMapper.selectCount(positionQuery)));
        map.put("appCount", safeCount("company.appCount", () -> applicationMapper.countByCompanyId(companyId)));
        map.put("assignmentCount", safeCount("company.assignmentCount", () -> assignmentMapper.selectCount(assignmentQuery)));
        map.put("logCount", countLogByStudentIds(studentIds));
        map.put("reportCount", countReportByStudentIds(studentIds));
        map.put("attendanceCount", countAttendanceByStudentIds(studentIds));
        map.put("noticeList", noticeMapper.selectLatestActive(5));
        map.put("todoCards", buildCompanyTodoCards(currentUser));
        return map;
    }

    private Map<String, Object> buildStudentStats(User currentUser) {
        Map<String, Object> map = baseScopedStatsTemplate();
        Long studentId = currentUser.getId();

        QueryWrapper<Log> logQuery = new QueryWrapper<>();
        logQuery.eq("student_id", studentId);

        QueryWrapper<Report> reportQuery = new QueryWrapper<>();
        reportQuery.eq("student_id", studentId);

        QueryWrapper<Attendance> attendanceQuery = new QueryWrapper<>();
        attendanceQuery.eq("student_id", studentId);

        Assignment activeAssignment = assignmentMapper.selectActiveByStudentId(studentId);

        map.put("userCount", 1);
        map.put("companyCount", activeAssignment == null || activeAssignment.getCompanyId() == null ? 0 : 1);
        map.put("positionCount", activeAssignment == null || activeAssignment.getPositionId() == null ? 0 : 1);
        map.put("appCount", safeCount("student.appCount", () -> applicationMapper.countByStudentId(studentId)));
        map.put("assignmentCount", safeCount("student.assignmentCount", () -> assignmentMapper.countActiveByStudentId(studentId)));
        map.put("logCount", logMapper.selectCount(logQuery));
        map.put("reportCount", reportMapper.selectCount(reportQuery));
        map.put("attendanceCount", attendanceMapper.selectCount(attendanceQuery));
        map.put("noticeList", noticeMapper.selectLatestActive(5));
        map.put("todoCards", buildStudentTodoCards(currentUser));
        return map;
    }

    private Map<String, Object> baseScopedStatsTemplate() {
        Map<String, Object> map = new HashMap<>();
        map.put("userRoleChart", new ArrayList<>());
        map.put("applicationStatusChart", new ArrayList<>());
        map.put("topPositionChart", new ArrayList<>());

        List<String> dates = last7Dates();
        map.put("trendDates", dates);
        map.put("applyTrend", zeroTrend(dates.size()));
        map.put("checkinTrend", zeroTrend(dates.size()));
        map.put("logTrend", zeroTrend(dates.size()));
        return map;
    }

    private List<Map<String, Object>> buildAdminTodoCards() {
        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(todoCard("待指派审核教师", safeCount("admin.todo.assignTeacher", () -> applicationMapper.countByStatusAndNoReviewTeacher(1)), "/audit", "企业已通过但未分配到教师"));
        cards.add(todoCard("待管理员终审", safeCount("admin.todo.finalReview", () -> applicationMapper.countByStatus(2)), "/audit", "教师已通过等待终审"));
        cards.add(todoCard("待分配申请", safeCount("admin.todo.pendingAssign", () -> applicationMapper.countByStatus(4)), "/assignment", "终审通过待分配指导教师"));
        cards.add(todoCard("待管理员复议", safeCount("admin.todo.appeal", appealMapper::countPendingForAdmin), "/appeal", "教师初审通过待复议"));
        cards.add(todoCard("待处理异常", safeCount("admin.todo.incident", incidentMapper::countPendingForAdmin), "/incident", "异常工单待处理/处理中"));
        return cards;
    }

    private List<Map<String, Object>> buildTeacherTodoCards(User currentUser) {
        Long teacherId = currentUser.getId();
        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(todoCard("待审核申请", safeCount("teacher.todo.review", () -> applicationMapper.countByReviewTeacherIdAndStatus(teacherId, 1)), "/audit", "仅显示管理员已指派给你的申请"));
        cards.add(todoCard("待批阅日志", safeCount("teacher.todo.log", () -> logMapper.countPendingByTeacher(teacherId)), "/log", "所属学生日志未评分"));
        cards.add(todoCard("待批阅报告", safeCount("teacher.todo.report", () -> reportMapper.countPendingByTeacher(teacherId)), "/report", "所属学生报告待批阅"));
        cards.add(todoCard("待处理异常", safeCount("teacher.todo.incident", () -> incidentMapper.countPendingByTeacher(teacherId)), "/incident", "你负责学生相关异常"));
        cards.add(todoCard("待初审申诉", safeCount("teacher.todo.appeal", () -> appealMapper.countPendingByTeacher(teacherId)), "/appeal", "待教师初审的申诉"));
        return cards;
    }

    private List<Map<String, Object>> buildCompanyTodoCards(User currentUser) {
        Long companyId = currentUser.getCompanyId();
        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(todoCard("待企业预审", safeCount("company.todo.review", () -> applicationMapper.countByCompanyIdAndStatus(companyId, 0)), "/audit", "企业岗位的新申请待预审"));
        cards.add(todoCard("待分配导师", safeCount("company.todo.mentor", () -> assignmentMapper.countActiveWithoutMentorByCompanyId(companyId)), "/assignment", "在岗学生尚未绑定企业导师"));
        cards.add(todoCard("待处理异常", safeCount("company.todo.incident", () -> incidentMapper.countPendingByCompany(companyId, currentUser.getId())), "/incident", "企业相关异常待跟进"));
        return cards;
    }

    private List<Map<String, Object>> buildStudentTodoCards(User currentUser) {
        Long studentId = currentUser.getId();

        QueryWrapper<Application> pendingApplicationQuery = new QueryWrapper<>();
        pendingApplicationQuery.eq("student_id", studentId).in("status", List.of(0, 1, 2, 4));

        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(todoCard("申请处理中", safeCount("student.todo.application", () -> applicationMapper.selectCount(pendingApplicationQuery)), "/application", "企业/教师/管理员审核中的申请"));
        cards.add(todoCard("进行中分配", safeCount("student.todo.assignment", () -> assignmentMapper.countActiveByStudentId(studentId)), "/assignment", "当前有效的实习分配"));
        cards.add(todoCard("日志待批阅", safeCount("student.todo.log", () -> logMapper.countPendingByStudent(studentId)), "/log", "已提交但尚未评分"));
        cards.add(todoCard("报告待批阅", safeCount("student.todo.report", () -> reportMapper.countPendingByStudent(studentId)), "/report", "已提交但尚未批阅"));
        cards.add(todoCard("协议待审核", safeCount("student.todo.agreement", () -> agreementMapper.countPendingByStudent(studentId)), "/agreement", "上传后待教师审核"));
        cards.add(todoCard("申诉处理中", safeCount("student.todo.appeal", () -> appealMapper.countInProgressByStudent(studentId)), "/appeal", "已提交未完成的申诉"));
        return cards;
    }

    private Map<String, Object> todoCard(String title, long value, String route, String description) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("value", value);
        map.put("route", route);
        map.put("description", description);
        return map;
    }

    private long safeCount(String key, Supplier<Long> supplier) {
        try {
            Long value = supplier.get();
            return value == null ? 0L : value;
        } catch (Exception ex) {
            log.warn("stats count fallback for {}: {}", key, ex.getMessage());
            return 0L;
        }
    }

    private long countApplicationByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return 0L;
        }
        QueryWrapper<Application> query = new QueryWrapper<>();
        query.in("student_id", studentIds);
        return applicationMapper.selectCount(query);
    }

    private long countLogByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return 0L;
        }
        QueryWrapper<Log> query = new QueryWrapper<>();
        query.in("student_id", studentIds);
        return logMapper.selectCount(query);
    }

    private long countReportByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return 0L;
        }
        QueryWrapper<Report> query = new QueryWrapper<>();
        query.in("student_id", studentIds);
        return reportMapper.selectCount(query);
    }

    private long countAttendanceByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return 0L;
        }
        QueryWrapper<Attendance> query = new QueryWrapper<>();
        query.in("student_id", studentIds);
        return attendanceMapper.selectCount(query);
    }

    private long countAssignmentByTeacher(Long teacherId) {
        QueryWrapper<Assignment> query = new QueryWrapper<>();
        query.eq("teacher_id", teacherId).eq("status", 1);
        return assignmentMapper.selectCount(query);
    }

    private List<Long> zeroTrend(int size) {
        List<Long> data = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            data.add(0L);
        }
        return data;
    }

    private List<String> last7Dates() {
        List<String> dates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            dates.add(LocalDate.now().minusDays(i).format(formatter));
        }
        return dates;
    }

    private Map<String, Long> toTrendMap(List<Map<String, Object>> rawList) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> item : rawList) {
            if (item == null) {
                continue;
            }
            Object nameObj = item.get("name");
            Object valueObj = item.get("value");
            if (nameObj == null || valueObj == null) {
                continue;
            }
            String name = String.valueOf(nameObj);
            Long value;
            if (valueObj instanceof Number number) {
                value = number.longValue();
            } else {
                try {
                    value = Long.parseLong(String.valueOf(valueObj));
                } catch (NumberFormatException e) {
                    value = 0L;
                }
            }
            map.put(name, value);
        }
        return map;
    }
}
