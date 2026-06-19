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
import fun.bmoqing.internship.entity.Incident;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.IncidentMapper;
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
@RequestMapping("/api/incident")
public class IncidentController {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_RESOLVED = 2;
    private static final int STATUS_REJECTED = 3;

    private static final Set<String> TYPE_SET = Set.of("SAFETY", "ATTENDANCE", "DISCIPLINE", "TASK", "OTHER");
    private static final Set<String> LEVEL_SET = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final IncidentMapper incidentMapper;
    private final AssignmentMapper assignmentMapper;
    private final InternshipRecordService internshipRecordService;
    private final AuditLogService auditLogService;

    public IncidentController(IncidentMapper incidentMapper,
                              AssignmentMapper assignmentMapper,
                              InternshipRecordService internshipRecordService,
                              AuditLogService auditLogService) {
        this.incidentMapper = incidentMapper;
        this.assignmentMapper = assignmentMapper;
        this.internshipRecordService = internshipRecordService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword,
                          @RequestParam(required = false) Integer status) {
        Page<Incident> page = new Page<>(pageNum, pageSize);

        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(incidentMapper.selectPageForAdmin(page, keyword, status));
        }
        if (AuthUtil.hasRole("TEACHER")) {
            return Result.success(incidentMapper.selectPageForTeacher(page, keyword, status, AuthUtil.currentUserId()));
        }
        if (AuthUtil.hasRole("COMPANY")) {
            User user = AuthUtil.currentUser();
            Long companyId = user == null ? null : user.getCompanyId();
            return Result.success(incidentMapper.selectPageForCompany(page, keyword, status, companyId, AuthUtil.currentUserId()));
        }
        if (AuthUtil.hasRole("STUDENT")) {
            return Result.success(incidentMapper.selectPageForStudent(page, status, AuthUtil.currentUserId()));
        }

        return Result.forbidden("无权限访问异常事件列表");
    }

    @PostMapping("/report")
    public Result<?> report(@RequestBody Incident request) {
        if (!AuthUtil.hasRole("ADMIN", "TEACHER", "STUDENT", "COMPANY")) {
            return Result.forbidden("无权限上报异常事件");
        }

        if (!StringUtils.hasText(request.getTitle())) {
            return Result.validationError("异常标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            return Result.validationError("异常描述不能为空");
        }
        if (request.getTitle().length() > 120) {
            return Result.validationError("异常标题不能超过120个字符");
        }
        if (request.getContent().length() > 1000) {
            return Result.validationError("异常描述不能超过1000个字符");
        }

        Assignment assignment = null;
        if (request.getAssignmentId() != null) {
            assignment = assignmentMapper.selectById(request.getAssignmentId());
            if (assignment == null) {
                return Result.notFound("分配记录不存在");
            }
        } else if (!AuthUtil.hasRole("ADMIN")) {
            return Result.validationError("非管理员上报需指定分配ID");
        }

        User currentUser = AuthUtil.currentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Result.unauthorized("登录状态异常，请重新登录");
        }

        Long studentId = request.getStudentId();
        if (AuthUtil.hasRole("STUDENT") && !AuthUtil.hasRole("ADMIN")) {
            if (assignment == null || !currentUser.getId().equals(assignment.getStudentId())) {
                return Result.forbidden("仅可上报自己的实习异常");
            }
            studentId = currentUser.getId();
        } else if (AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN")) {
            if (assignment == null || !currentUser.getId().equals(assignment.getTeacherId())) {
                return Result.forbidden("仅可上报自己指导学生的异常");
            }
            studentId = assignment.getStudentId();
        } else if (AuthUtil.hasRole("COMPANY") && !AuthUtil.hasRole("ADMIN")) {
            if (assignment == null) {
                return Result.validationError("企业上报需指定分配ID");
            }
            boolean companyMatched = currentUser.getCompanyId() != null
                    && assignment.getCompanyId() != null
                    && currentUser.getCompanyId().equals(assignment.getCompanyId());
            boolean mentorMatched = assignment.getMentorId() != null
                    && assignment.getMentorId().equals(currentUser.getId());
            if (!companyMatched && !mentorMatched) {
                return Result.forbidden("仅可上报本企业负责学生的异常");
            }
            studentId = assignment.getStudentId();
        } else if (assignment != null) {
            studentId = assignment.getStudentId();
        }

        if (studentId == null) {
            return Result.validationError("未确定异常关联学生，请补充分配ID或学生ID");
        }

        String type = normalizeType(request.getType());
        if (!TYPE_SET.contains(type)) {
            return Result.validationError("异常类型无效，仅支持：SAFETY/ATTENDANCE/DISCIPLINE/TASK/OTHER");
        }

        String level = normalizeLevel(request.getLevel());
        if (!LEVEL_SET.contains(level)) {
            return Result.validationError("异常等级无效，仅支持：LOW/MEDIUM/HIGH/CRITICAL");
        }

        Incident incident = new Incident();
        incident.setAssignmentId(assignment == null ? null : assignment.getId());
        incident.setStudentId(studentId);
        incident.setReporterId(currentUser.getId());
        incident.setReporterRole(currentUser.getRole());
        incident.setType(type);
        incident.setLevel(level);
        incident.setTitle(request.getTitle());
        incident.setContent(request.getContent());
        incident.setStatus(STATUS_PENDING);
        incident.setReportTime(LocalDateTime.now());
        incident.setUpdateTime(LocalDateTime.now());
        incidentMapper.insert(incident);

        internshipRecordService.addRecord(
                studentId,
                "INCIDENT_REPORT",
                "上报异常事件：" + incident.getTitle(),
                incident.getId()
        );

        auditLogService.record(
                "INCIDENT_REPORT",
                "INCIDENT",
                incident.getId(),
                "学生ID=" + studentId + "，类型=" + type + "，等级=" + level + "，标题=" + incident.getTitle()
        );

        return Result.success(incident);
    }

    @PutMapping("/handle")
    public Result<?> handle(@RequestBody Incident request) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师/管理员可处理异常事件");
        }

        if (request.getId() == null) {
            return Result.validationError("异常ID不能为空");
        }
        if (request.getStatus() == null) {
            return Result.validationError("处理状态不能为空");
        }
        if (request.getStatus() != STATUS_PROCESSING
                && request.getStatus() != STATUS_RESOLVED
                && request.getStatus() != STATUS_REJECTED) {
            return Result.validationError("处理状态仅支持：1处理中、2已解决、3已驳回");
        }
        if (!StringUtils.hasText(request.getHandleResult())) {
            return Result.validationError("处理结果不能为空");
        }
        if (request.getHandleResult().length() > 500) {
            return Result.validationError("处理结果不能超过500个字符");
        }

        Incident dbIncident = incidentMapper.selectById(request.getId());
        if (dbIncident == null) {
            return Result.notFound("异常记录不存在");
        }
        if (dbIncident.getStatus() != null
                && (dbIncident.getStatus() == STATUS_RESOLVED || dbIncident.getStatus() == STATUS_REJECTED)) {
            return Result.validationError("该异常已结束，不能重复处理");
        }

        if (AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN")) {
            boolean permitted = false;
            if (dbIncident.getReporterId() != null && dbIncident.getReporterId().equals(AuthUtil.currentUserId())) {
                permitted = true;
            }
            if (!permitted && dbIncident.getAssignmentId() != null) {
                Assignment assignment = assignmentMapper.selectById(dbIncident.getAssignmentId());
                permitted = assignment != null && AuthUtil.currentUserId().equals(assignment.getTeacherId());
            }
            if (!permitted && dbIncident.getStudentId() != null) {
                permitted = assignmentMapper.countTeacherStudentRelation(AuthUtil.currentUserId(), dbIncident.getStudentId()) > 0;
            }
            if (!permitted) {
                return Result.forbidden("仅可处理自己负责学生的异常事件");
            }
        }

        Incident update = new Incident();
        update.setId(request.getId());
        update.setStatus(request.getStatus());
        update.setHandlerId(AuthUtil.currentUserId());
        update.setHandleResult(request.getHandleResult());
        update.setHandleTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        incidentMapper.updateById(update);

        if (dbIncident.getStudentId() != null) {
            internshipRecordService.addRecord(
                    dbIncident.getStudentId(),
                    "INCIDENT_HANDLE",
                    "异常事件已处理，状态=" + request.getStatus(),
                    dbIncident.getId()
            );
        }

        auditLogService.record(
                "INCIDENT_HANDLE",
                "INCIDENT",
                dbIncident.getId(),
                "处理状态=" + request.getStatus() + "，结果=" + request.getHandleResult()
        );

        return Result.success(null);
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return "OTHER";
        }
        return type.trim().toUpperCase();
    }

    private String normalizeLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return "MEDIUM";
        }
        return level.trim().toUpperCase();
    }
}
