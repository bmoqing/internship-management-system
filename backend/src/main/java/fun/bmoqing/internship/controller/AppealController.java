/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Appeal;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.mapper.AppealMapper;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.service.AuditLogService;
import fun.bmoqing.internship.service.InternshipRecordService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/appeal")
public class AppealController {

    private static final int STATUS_PENDING_TEACHER = 0;
    private static final int STATUS_PENDING_ADMIN = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_CLOSED = 4;

    private static final Set<String> TARGET_TYPE_SET = Set.of("SCORE", "REPORT", "LOG", "ATTENDANCE", "OTHER");

    private final AppealMapper appealMapper;
    private final AssignmentMapper assignmentMapper;
    private final InternshipRecordService internshipRecordService;
    private final AuditLogService auditLogService;

    public AppealController(AppealMapper appealMapper,
                            AssignmentMapper assignmentMapper,
                            InternshipRecordService internshipRecordService,
                            AuditLogService auditLogService) {
        this.appealMapper = appealMapper;
        this.assignmentMapper = assignmentMapper;
        this.internshipRecordService = internshipRecordService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/submit")
    public Result<?> submit(@RequestBody Appeal request) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("仅学生可发起申诉");
        }

        if (request.getAssignmentId() == null) {
            return Result.validationError("申诉需关联分配记录");
        }
        if (!StringUtils.hasText(request.getReason())) {
            return Result.validationError("申诉理由不能为空");
        }
        if (request.getReason().length() > 1000) {
            return Result.validationError("申诉理由不能超过1000个字符");
        }
        if (request.getEvidenceUrl() != null && request.getEvidenceUrl().length() > 500) {
            return Result.validationError("证据链接不能超过500个字符");
        }

        Assignment assignment = assignmentMapper.selectById(request.getAssignmentId());
        if (assignment == null) {
            return Result.notFound("分配记录不存在");
        }
        if (!AuthUtil.currentUserId().equals(assignment.getStudentId())) {
            return Result.forbidden("仅可为自己的实习发起申诉");
        }
        if (assignment.getStatus() == null || assignment.getStatus() != 1) {
            return Result.validationError("仅可对进行中的实习发起申诉");
        }

        String targetType = normalizeTargetType(request.getTargetType());
        if (!TARGET_TYPE_SET.contains(targetType)) {
            return Result.validationError("申诉对象类型无效，仅支持：SCORE/REPORT/LOG/ATTENDANCE/OTHER");
        }

        Appeal appeal = new Appeal();
        appeal.setStudentId(AuthUtil.currentUserId());
        appeal.setAssignmentId(assignment.getId());
        appeal.setTargetType(targetType);
        appeal.setTargetId(request.getTargetId());
        appeal.setReason(request.getReason());
        appeal.setEvidenceUrl(request.getEvidenceUrl());
        appeal.setStatus(STATUS_PENDING_TEACHER);
        appeal.setCreateTime(LocalDateTime.now());
        appeal.setUpdateTime(LocalDateTime.now());
        appealMapper.insert(appeal);

        internshipRecordService.addRecord(
                appeal.getStudentId(),
                "APPEAL_SUBMIT",
                "发起申诉，类型=" + targetType,
                appeal.getId()
        );

        auditLogService.record(
                "APPEAL_SUBMIT",
                "APPEAL",
                appeal.getId(),
                "学生ID=" + appeal.getStudentId() + "，对象类型=" + targetType
        );

        return Result.success(appeal);
    }

    @GetMapping("/my")
    public Result<?> my(@RequestParam(defaultValue = "1") Integer pageNum,
                        @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("仅学生可查看个人申诉");
        }

        Page<Appeal> page = new Page<>(pageNum, pageSize);
        return Result.success(appealMapper.selectPageForStudent(page, AuthUtil.currentUserId()));
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword,
                          @RequestParam(required = false) Integer status) {
        Page<Appeal> page = new Page<>(pageNum, pageSize);

        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(appealMapper.selectPageForAdmin(page, keyword, status));
        }
        if (AuthUtil.hasRole("TEACHER")) {
            return Result.success(appealMapper.selectPageForTeacher(page, keyword, status, AuthUtil.currentUserId()));
        }
        return Result.forbidden("仅教师/管理员可查看申诉列表");
    }

    @PutMapping("/teacher-review")
    public Result<?> teacherReview(@RequestBody Appeal request) {
        if (!AuthUtil.hasRole("TEACHER")) {
            return Result.forbidden("仅教师可执行初审");
        }

        if (request.getId() == null) {
            return Result.validationError("申诉ID不能为空");
        }
        if (request.getStatus() == null) {
            return Result.validationError("处理状态不能为空");
        }
        if (request.getStatus() != STATUS_PENDING_ADMIN && request.getStatus() != STATUS_REJECTED) {
            return Result.validationError("教师初审仅支持：转管理员复议(1) 或驳回(3)");
        }
        if (request.getTeacherReply() != null && request.getTeacherReply().length() > 255) {
            return Result.validationError("教师回复不能超过255个字符");
        }

        Appeal dbAppeal = appealMapper.selectById(request.getId());
        if (dbAppeal == null) {
            return Result.notFound("申诉记录不存在");
        }
        if (dbAppeal.getStatus() == null || dbAppeal.getStatus() != STATUS_PENDING_TEACHER) {
            return Result.validationError("当前状态不允许教师初审");
        }

        Assignment assignment = assignmentMapper.selectById(dbAppeal.getAssignmentId());
        if (assignment == null || !AuthUtil.currentUserId().equals(assignment.getTeacherId())) {
            return Result.forbidden("仅可初审自己指导学生的申诉");
        }

        Appeal update = new Appeal();
        update.setId(dbAppeal.getId());
        update.setStatus(request.getStatus());
        update.setTeacherId(AuthUtil.currentUserId());
        update.setTeacherReply(request.getTeacherReply());
        update.setTeacherReviewTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        appealMapper.updateById(update);

        internshipRecordService.addRecord(
                dbAppeal.getStudentId(),
                "APPEAL_TEACHER_REVIEW",
                request.getStatus() == STATUS_PENDING_ADMIN ? "申诉教师初审通过，待管理员复议" : "申诉教师初审驳回",
                dbAppeal.getId()
        );

        auditLogService.record(
                "APPEAL_TEACHER_REVIEW",
                "APPEAL",
                dbAppeal.getId(),
                "结果=" + (request.getStatus() == STATUS_PENDING_ADMIN ? "转复议" : "驳回")
        );

        return Result.success(null);
    }

    @PutMapping("/admin-review")
    public Result<?> adminReview(@RequestBody Appeal request) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可执行复议");
        }

        if (request.getId() == null) {
            return Result.validationError("申诉ID不能为空");
        }
        if (request.getStatus() == null) {
            return Result.validationError("复议状态不能为空");
        }
        if (request.getStatus() != STATUS_APPROVED && request.getStatus() != STATUS_REJECTED) {
            return Result.validationError("管理员复议仅支持：通过(2)或驳回(3)");
        }
        if (request.getAdminReply() != null && request.getAdminReply().length() > 255) {
            return Result.validationError("管理员回复不能超过255个字符");
        }

        Appeal dbAppeal = appealMapper.selectById(request.getId());
        if (dbAppeal == null) {
            return Result.notFound("申诉记录不存在");
        }
        if (dbAppeal.getStatus() == null || dbAppeal.getStatus() != STATUS_PENDING_ADMIN) {
            return Result.validationError("当前状态不允许管理员复议");
        }

        Appeal update = new Appeal();
        update.setId(dbAppeal.getId());
        update.setStatus(request.getStatus());
        update.setAdminId(AuthUtil.currentUserId());
        update.setAdminReply(request.getAdminReply());
        update.setAdminReviewTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        appealMapper.updateById(update);

        internshipRecordService.addRecord(
                dbAppeal.getStudentId(),
                "APPEAL_ADMIN_REVIEW",
                request.getStatus() == STATUS_APPROVED ? "申诉复议通过" : "申诉复议驳回",
                dbAppeal.getId()
        );

        auditLogService.record(
                "APPEAL_ADMIN_REVIEW",
                "APPEAL",
                dbAppeal.getId(),
                "复议结果=" + (request.getStatus() == STATUS_APPROVED ? "通过" : "驳回")
        );

        return Result.success(null);
    }

    @PutMapping("/close")
    public Result<?> close(@RequestBody Appeal request) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("仅学生可关闭申诉");
        }

        if (request.getId() == null) {
            return Result.validationError("申诉ID不能为空");
        }

        Appeal dbAppeal = appealMapper.selectById(request.getId());
        if (dbAppeal == null) {
            return Result.notFound("申诉记录不存在");
        }
        if (!AuthUtil.currentUserId().equals(dbAppeal.getStudentId())) {
            return Result.forbidden("仅可关闭自己的申诉记录");
        }
        if (dbAppeal.getStatus() == null || dbAppeal.getStatus() != STATUS_APPROVED) {
            return Result.validationError("仅复议通过的申诉可关闭");
        }

        Appeal update = new Appeal();
        update.setId(dbAppeal.getId());
        update.setStatus(STATUS_CLOSED);
        update.setCloseTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        appealMapper.updateById(update);

        internshipRecordService.addRecord(
                dbAppeal.getStudentId(),
                "APPEAL_CLOSE",
                "申诉流程已关闭",
                dbAppeal.getId()
        );

        auditLogService.record(
                "APPEAL_CLOSE",
                "APPEAL",
                dbAppeal.getId(),
                "学生确认关闭申诉流程"
        );

        return Result.success(null);
    }

    private String normalizeTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            return "OTHER";
        }
        return targetType.trim().toUpperCase();
    }
}
