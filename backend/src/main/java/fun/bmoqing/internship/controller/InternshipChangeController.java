package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Application;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.entity.InternshipChange;
import fun.bmoqing.internship.entity.Position;
import fun.bmoqing.internship.mapper.ApplicationMapper;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.InternshipChangeMapper;
import fun.bmoqing.internship.mapper.PositionMapper;
import fun.bmoqing.internship.service.AuditLogService;
import fun.bmoqing.internship.service.InternshipRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/change")
public class InternshipChangeController {

    @Autowired
    private InternshipChangeMapper changeMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private InternshipRecordService recordService;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/apply")
    public Result<?> apply(@RequestBody InternshipChange change) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("仅学生可发起变更申请");
        }

        Long studentId = AuthUtil.currentUserId();
        Assignment activeAssignment = assignmentMapper.selectActiveByStudentId(studentId);
        if (activeAssignment == null) {
            return Result.validationError("当前无进行中的实习，无法发起变更");
        }

        // Check for existing pending changes
        QueryWrapper<InternshipChange> query = new QueryWrapper<>();
        query.eq("student_id", studentId).in("status", 0, 1);
        if (changeMapper.selectCount(query) > 0) {
            return Result.conflict("您已有正在处理中的变更申请，请等待处理完毕");
        }

        change.setStudentId(studentId);
        change.setAssignmentId(activeAssignment.getId());
        change.setCompanyId(activeAssignment.getCompanyId());
        change.setTeacherId(activeAssignment.getTeacherId());
        change.setStatus(0); // 待企业审核
        change.setCreateTime(LocalDateTime.now());
        change.setUpdateTime(LocalDateTime.now());

        if (change.getType() == 1 || change.getType() == 2) {
            if (change.getTargetPositionId() == null) {
                return Result.validationError("转岗或换企业必须指定目标岗位");
            }
            Position p = positionMapper.selectById(change.getTargetPositionId());
            if (p == null) return Result.notFound("目标岗位不存在");
            change.setTargetCompanyId(p.getCompanyId());
        }

        changeMapper.insert(change);

        recordService.addRecord(
                studentId,
                "CHANGE_APPLY",
                "发起实习变更申请，类型：" + (change.getType() == 1 ? "转岗" : change.getType() == 2 ? "换企业" : "提前离职"),
                change.getId()
        );

        return Result.success(null);
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword) {
        Page<InternshipChange> page = new Page<>(pageNum, pageSize);
        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(changeMapper.selectAdminPage(page, keyword));
        }
        if (AuthUtil.hasRole("TEACHER")) {
            return Result.success(changeMapper.selectTeacherPage(page, keyword, AuthUtil.currentUserId()));
        }
        if (AuthUtil.hasRole("COMPANY")) {
            Long companyId = AuthUtil.currentUser().getCompanyId();
            if (companyId == null) {
                return Result.error("企业账号未绑定企业信息");
            }
            return Result.success(changeMapper.selectCompanyPage(page, keyword, companyId));
        }
        if (AuthUtil.hasRole("STUDENT")) {
            return Result.success(changeMapper.selectStudentPage(page, AuthUtil.currentUserId()));
        }
        return Result.forbidden("无权限");
    }

    @PutMapping("/company-audit")
    public Result<?> companyAudit(@RequestBody InternshipChange auditData) {
        if (!AuthUtil.hasRole("COMPANY")) {
            return Result.forbidden("仅企业可进行预审");
        }
        InternshipChange change = changeMapper.selectById(auditData.getId());
        if (change == null || change.getStatus() != 0) {
            return Result.error("工单状态异常或不存在");
        }
        if (!AuthUtil.currentUser().getCompanyId().equals(change.getCompanyId())) {
            return Result.forbidden("无权限审核该工单");
        }

        change.setStatus(auditData.getStatus()); // 1 or 3
        change.setCompanyRemark(auditData.getCompanyRemark());
        change.setUpdateTime(LocalDateTime.now());
        changeMapper.updateById(change);

        auditLogService.record("CHANGE_COMPANY_AUDIT", "CHANGE", change.getId(), "状态变更为：" + change.getStatus());

        return Result.success(null);
    }

    @PutMapping("/teacher-audit")
    public Result<?> teacherAudit(@RequestBody InternshipChange auditData) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师或管理员可进行终审");
        }
        InternshipChange change = changeMapper.selectById(auditData.getId());
        if (change == null || change.getStatus() != 1) {
            return Result.error("工单状态异常或尚未经过企业确认");
        }

        change.setStatus(auditData.getStatus()); // 2 or 3
        change.setTeacherRemark(auditData.getTeacherRemark());
        change.setUpdateTime(LocalDateTime.now());
        changeMapper.updateById(change);

        if (change.getStatus() == 2) {
            // End old assignment
            Assignment oldAss = assignmentMapper.selectById(change.getAssignmentId());
            if (oldAss != null) {
                oldAss.setStatus(2); // 已结束
                assignmentMapper.updateById(oldAss);
            }
            // Create new application if needed
            if ((change.getType() == 1 || change.getType() == 2) && change.getTargetPositionId() != null) {
                Application app = new Application();
                app.setStudentId(change.getStudentId());
                app.setPositionId(change.getTargetPositionId());
                app.setStatus(0);
                app.setApplyTime(LocalDateTime.now());
                applicationMapper.insert(app);
            }
        }

        auditLogService.record("CHANGE_TEACHER_AUDIT", "CHANGE", change.getId(), "终审变更为：" + change.getStatus());
        recordService.addRecord(
                change.getStudentId(),
                "CHANGE_FINISH",
                "实习变更审核完成，结果：" + (change.getStatus() == 2 ? "通过" : "驳回"),
                change.getId()
        );

        return Result.success(null);
    }
}
