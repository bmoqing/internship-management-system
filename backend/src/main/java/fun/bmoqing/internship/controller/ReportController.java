/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.entity.Report;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.ReportMapper;
import fun.bmoqing.internship.service.AuditLogService;
import fun.bmoqing.internship.service.InternshipRecordService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportMapper reportMapper;
    private final AssignmentMapper assignmentMapper;
    private final InternshipRecordService internshipRecordService;
    private final AuditLogService auditLogService;

    public ReportController(ReportMapper reportMapper,
                            AssignmentMapper assignmentMapper,
                            InternshipRecordService internshipRecordService,
                            AuditLogService auditLogService) {
        this.reportMapper = reportMapper;
        this.assignmentMapper = assignmentMapper;
        this.internshipRecordService = internshipRecordService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public Result<?> submit(@RequestBody Report report) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以提交实习报告");
        }

        if (report.getTitle() == null || report.getTitle().isBlank()) {
            return Result.error("报告标题不能为空");
        }
        if (report.getContent() == null || report.getContent().isBlank()) {
            return Result.error("报告内容不能为空");
        }
        if (report.getTitle().length() > 120) {
            return Result.error("报告标题不能超过120个字符");
        }
        if (report.getTeacherComment() != null) {
            report.setTeacherComment(null);
        }
        if (report.getScore() != null) {
            report.setScore(null);
        }

        Long studentId = AuthUtil.currentUserId();
        Assignment activeAssignment = assignmentMapper.selectActiveByStudentId(studentId);
        if (activeAssignment == null) {
            return Result.validationError("当前无进行中实习分配，暂不能提交报告");
        }

        report.setAssignmentId(activeAssignment.getId());
        report.setStudentId(studentId);
        report.setStatus(1);
        report.setSubmitTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.insert(report);

        internshipRecordService.addRecord(
                report.getStudentId(),
                "REPORT_SUBMIT",
                "提交实习报告：" + report.getTitle(),
                report.getId()
        );

        return Result.success(null);
    }

    @GetMapping("/my")
    public Result<?> myReports(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以查看个人报告");
        }

        Page<Report> page = new Page<>(pageNum, pageSize);
        return Result.success(reportMapper.selectPageForStudent(page, AuthUtil.currentUserId()));
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("ADMIN", "TEACHER")) {
            return Result.forbidden("仅管理员/教师可查看报告列表");
        }

        Page<Report> page = new Page<>(pageNum, pageSize);
        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(reportMapper.selectPageForAdmin(page, keyword));
        }
        return Result.success(reportMapper.selectPageForTeacher(page, keyword, AuthUtil.currentUserId()));
    }

    @PutMapping("/review")
    public Result<?> review(@RequestBody Report report) {
        if (!AuthUtil.hasRole("ADMIN", "TEACHER")) {
            return Result.forbidden("仅管理员/教师可批阅报告");
        }

        if (report.getId() == null) {
            return Result.error("报告ID不能为空");
        }
        if (report.getScore() == null) {
            return Result.error("报告评分不能为空");
        }
        if (report.getScore() < 0 || report.getScore() > 100) {
            return Result.error("报告评分需在0-100之间");
        }
        if (report.getTeacherComment() != null && report.getTeacherComment().length() > 255) {
            return Result.error("教师评语不能超过255个字符");
        }

        Report dbReport = reportMapper.selectById(report.getId());
        if (dbReport == null) {
            return Result.error("报告不存在");
        }
        if (dbReport.getStatus() != null && dbReport.getStatus() != 1 && dbReport.getStatus() != 3) {
            return Result.error("该报告已批阅且未被打回，不可重复批阅");
        }

        if (AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN")) {
            long relationCount = assignmentMapper.countTeacherStudentRelation(AuthUtil.currentUserId(), dbReport.getStudentId());
            if (relationCount <= 0) {
                return Result.forbidden("仅可批阅自己指导学生的报告");
            }
        }

        Report update = new Report();
        update.setId(report.getId());
        update.setStatus(2);
        update.setScore(report.getScore());
        update.setTeacherComment(report.getTeacherComment());
        update.setReviewerId(AuthUtil.currentUserId());
        update.setReviewTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(update);

        internshipRecordService.addRecord(
                dbReport.getStudentId(),
                "REPORT_REVIEW",
                "实习报告已批阅，评分：" + report.getScore(),
                dbReport.getId()
        );

        auditLogService.record(
                "REPORT_REVIEW",
                "REPORT",
                dbReport.getId(),
                "学生ID=" + dbReport.getStudentId() + "，评分=" + report.getScore()
        );

        return Result.success(null);
    }

    @PutMapping("/revoke")
    public Result<?> revoke(@RequestBody Report report) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师/管理员可打回报告");
        }

        if (report.getId() == null) {
            return Result.error("报告ID不能为空");
        }

        Report dbReport = reportMapper.selectById(report.getId());
        if (dbReport == null) {
            return Result.error("报告不存在");
        }
        if (dbReport.getStatus() == null || dbReport.getStatus() != 2) {
            return Result.error("仅已批阅状态的报告可被打回");
        }

        if (AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN")) {
            long relationCount = assignmentMapper.countTeacherStudentRelation(AuthUtil.currentUserId(), dbReport.getStudentId());
            if (relationCount <= 0) {
                return Result.forbidden("仅可打回自己指导学生的报告");
            }
        }

        Report update = new Report();
        update.setId(report.getId());
        update.setStatus(3); // 打回待修改
        update.setTeacherComment(report.getTeacherComment());
        update.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(update);

        internshipRecordService.addRecord(
                dbReport.getStudentId(),
                "REPORT_REVOKE",
                "实习报告被打回，请修改后重新提交",
                dbReport.getId()
        );

        auditLogService.record(
                "REPORT_REVOKE",
                "REPORT",
                dbReport.getId(),
                "学生ID=" + dbReport.getStudentId() +
                        (report.getTeacherComment() == null || report.getTeacherComment().isBlank()
                                ? ""
                                : "，原因=" + report.getTeacherComment())
        );

        return Result.success(null);
    }
}
