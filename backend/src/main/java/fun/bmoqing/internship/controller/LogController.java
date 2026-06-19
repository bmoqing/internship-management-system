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
import fun.bmoqing.internship.entity.Log;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.LogMapper;
import fun.bmoqing.internship.mapper.AgreementMapper;
import fun.bmoqing.internship.service.AuditLogService;
import fun.bmoqing.internship.service.InternshipRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/log")
public class LogController {

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private AgreementMapper agreementMapper;

    @Autowired
    private InternshipRecordService internshipRecordService;

    @Autowired
    private AuditLogService auditLogService;

    // 1. 学生提交日志
    @PostMapping
    public Result<?> save(@RequestBody Log log) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以提交日志");
        }

        if (!StringUtils.hasText(log.getTitle())) {
            return Result.error("日志标题不能为空");
        }
        if (!StringUtils.hasText(log.getContent())) {
            return Result.error("日志内容不能为空");
        }
        if (log.getTitle().length() > 100) {
            return Result.error("日志标题不能超过100个字符");
        }

        Long studentId = AuthUtil.currentUserId();
        Assignment activeAssignment = assignmentMapper.selectActiveByStudentId(studentId);
        if (activeAssignment == null) {
            return Result.validationError("当前无进行中实习分配，暂不能提交日志");
        }

        if (agreementMapper.countApprovedByAssignmentId(activeAssignment.getId()) == 0) {
            return Result.validationError("未完成三方协议签署及审核，无法进行实习活动，请先上传协议");
        }

        log.setStudentId(studentId);
        log.setAssignmentId(activeAssignment.getId());
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);

        internshipRecordService.addRecord(
                log.getStudentId(),
                "LOG_SUBMIT",
                "提交实习日志：" + log.getTitle(),
                log.getId()
        );

        return Result.success(null);
    }

    // 2. 学生查看自己的日志
    @GetMapping("/my")
    public Result<?> findMyLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) Long studentId) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以查看个人日志");
        }

        Page<Log> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Log> query = new QueryWrapper<>();
        query.eq("student_id", AuthUtil.currentUserId());
        query.orderByDesc("create_time");
        return Result.success(logMapper.selectPage(page, query));
    }

    // 3. 教师查看所有日志 (用于批阅)
    @GetMapping("/list")
    public Result<?> findList(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师/管理员可查看日志批阅列表");
        }

        Page<Log> page = new Page<>(pageNum, pageSize);
        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(logMapper.selectLogsWithStudent(page, keyword));
        }
        return Result.success(logMapper.selectLogsWithStudentByTeacher(page, keyword, AuthUtil.currentUserId()));
    }

    // 4. 教师批阅日志 (打分+评语)
    @PutMapping("/comment")
    public Result<?> comment(@RequestBody Log log) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师/管理员可批阅日志");
        }

        if (log.getId() == null) {
            return Result.error("日志ID不能为空");
        }

        Log dbLog = logMapper.selectById(log.getId());
        if (dbLog == null) {
            return Result.error("日志不存在");
        }

        if (AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN")) {
            long relationCount = assignmentMapper.countTeacherStudentRelation(AuthUtil.currentUserId(), dbLog.getStudentId());
            if (relationCount <= 0) {
                return Result.forbidden("仅可批阅自己指导学生的日志");
            }
        }

        if (log.getScore() != null && (log.getScore() < 0 || log.getScore() > 100)) {
            return Result.error("评分需在0-100之间");
        }
        if (log.getTeacherComment() != null && log.getTeacherComment().length() > 255) {
            return Result.error("评语不能超过255个字符");
        }

        Log update = new Log();
        update.setId(log.getId());
        update.setScore(log.getScore());
        update.setTeacherComment(log.getTeacherComment());
        logMapper.updateById(update);

        internshipRecordService.addRecord(
                dbLog.getStudentId(),
                "LOG_REVIEW",
                "实习日志已批阅，评分：" + (log.getScore() == null ? "未评分" : log.getScore()),
                dbLog.getId()
        );

        auditLogService.record(
                "LOG_REVIEW",
                "LOG",
                dbLog.getId(),
                "学生ID=" + dbLog.getStudentId() +
                        "，评分=" + (log.getScore() == null ? "未评分" : log.getScore())
        );

        return Result.success(null);
    }

    @PutMapping("/revoke")
    public Result<?> revoke(@RequestBody Log log) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师/管理员可打回日志");
        }

        if (log.getId() == null) {
            return Result.error("日志ID不能为空");
        }

        Log dbLog = logMapper.selectById(log.getId());
        if (dbLog == null) {
            return Result.error("日志不存在");
        }
        // 仅已批阅（有评分）的日志可打回
        if (dbLog.getScore() == null) {
            return Result.error("该日志尚未批阅，无需打回");
        }

        if (AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN")) {
            long relationCount = assignmentMapper.countTeacherStudentRelation(AuthUtil.currentUserId(), dbLog.getStudentId());
            if (relationCount <= 0) {
                return Result.forbidden("仅可打回自己指导学生的日志");
            }
        }

        Log update = new Log();
        update.setId(log.getId());
        update.setStatus(3); // 打回待修改
        update.setTeacherComment(log.getTeacherComment());
        logMapper.updateById(update);

        internshipRecordService.addRecord(
                dbLog.getStudentId(),
                "LOG_REVOKE",
                "实习日志被打回，请修改后重新提交",
                dbLog.getId()
        );

        auditLogService.record(
                "LOG_REVOKE",
                "LOG",
                dbLog.getId(),
                "学生ID=" + dbLog.getStudentId() +
                        (log.getTeacherComment() == null || log.getTeacherComment().isBlank()
                                ? ""
                                : "，原因=" + log.getTeacherComment())
        );

        return Result.success(null);
    }
}
