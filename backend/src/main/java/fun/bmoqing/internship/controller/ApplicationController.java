package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Application;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.entity.Company;
import fun.bmoqing.internship.entity.Position;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.ApplicationMapper;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.CompanyMapper;
import fun.bmoqing.internship.mapper.PositionMapper;
import fun.bmoqing.internship.mapper.UserMapper;
import fun.bmoqing.internship.service.AuditLogService;
import fun.bmoqing.internship.service.InternshipRecordService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/application")
public class ApplicationController {

    private static final int STATUS_PENDING_COMPANY_REVIEW = 0;
    private static final int STATUS_COMPANY_APPROVED = 1;
    private static final int STATUS_TEACHER_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_ADMIN_APPROVED = 4;
    private static final int STATUS_ASSIGNED = 5;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InternshipRecordService internshipRecordService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CompanyMapper companyMapper;

    // 1. 学生提交申请
    @PostMapping
    public Result<?> apply(@RequestBody Application application) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以提交申请");
        }

        if (application.getPositionId() == null) {
            return Result.validationError("岗位ID不能为空");
        }

        Position position = positionMapper.selectById(application.getPositionId());
        if (position == null) {
            return Result.notFound("岗位不存在");
        }
        if (position.getStatus() == null || position.getStatus() != 1) {
            return Result.validationError("该岗位已停止招聘，无法申请");
        }

        application.setStudentId(AuthUtil.currentUserId());

        User student = userMapper.selectById(application.getStudentId());
        if (student == null) {
            return Result.notFound("学生账号不存在");
        }
        if (student.getTeacherId() == null) {
            return Result.validationError("当前未绑定负责教师，请联系管理员在用户管理中绑定后再申请");
        }

        User reviewTeacher = userMapper.selectById(student.getTeacherId());
        if (reviewTeacher == null || !"TEACHER".equalsIgnoreCase(reviewTeacher.getRole())) {
            return Result.validationError("负责教师无效，请联系管理员修正师生绑定关系");
        }

        if (!StringUtils.hasText(student.getResumeUrl())) {
            return Result.validationError("请先在左侧菜单【我的简历】上传个人简历后再申请岗位");
        }

        if (assignmentMapper.countActiveByStudentId(application.getStudentId()) > 0) {
            return Result.conflict("你当前已有进行中的实习分配，不能重复申请新岗位");
        }

        // 简单校验：防止重复申请同一个岗位
        QueryWrapper<Application> query = new QueryWrapper<>();
        query.eq("student_id", application.getStudentId());
        query.eq("position_id", application.getPositionId());
        if (applicationMapper.selectCount(query) > 0) {
            return Result.conflict("你已经申请过该岗位了，请勿重复提交");
        }

        application.setStatus(STATUS_PENDING_COMPANY_REVIEW);
        application.setReviewTeacherId(student.getTeacherId());
        application.setApplyTime(LocalDateTime.now());
        applicationMapper.insert(application);

        internshipRecordService.addRecord(
                application.getStudentId(),
                "APPLICATION_SUBMIT",
                "提交岗位申请：" + position.getTitle(),
                application.getId()
        );

        return Result.success(null);
    }

    @PostMapping("/batch-audit")
    public Result<?> batchAudit(@RequestBody BatchAuditRequest request) {
        if (!AuthUtil.hasRole("ADMIN", "TEACHER", "COMPANY")) {
            return Result.forbidden("无权限执行此操作");
        }
        
        List<Long> ids = request.getIds();
        if (ids == null || ids.isEmpty()) {
            return Result.validationError("未选择任何申请记录");
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (Long id : ids) {
            try {
                ApplicationAuditRequest singleReq = new ApplicationAuditRequest();
                singleReq.setId(id);
                singleReq.setStatus(request.getStatus());
                singleReq.setReviewRemark(request.getReviewRemark());
                Result<?> res = this.audit(singleReq);
                if (res.getCode() == 200) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
            }
        }
        
        return Result.success("批量操作完成，成功：" + successCount + "条，失败：" + failCount + "条");
    }

    // 2. 学生查看自己的申请记录 (分页)
    @GetMapping("/my")
    public Result<?> findMyApplications(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) Long studentId) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以查看个人申请");
        }

        Page<Application> page = new Page<>(pageNum, pageSize);
        // 调用刚才在 Mapper 里写的自定义查询
        Page<Application> resultPage = applicationMapper.selectStudentApplications(page, AuthUtil.currentUserId());
        return Result.success(resultPage);

    }
    @GetMapping("/list")
    public Result<?> findListForReview(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(defaultValue = "") String keyword) {
        Page<Application> page = new Page<>(pageNum, pageSize);

        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(applicationMapper.selectAdminApplications(page, keyword));
        }

        if (AuthUtil.hasRole("TEACHER")) {
            return Result.success(applicationMapper.selectTeacherApplications(
                    page,
                    keyword,
                    AuthUtil.currentUserId()
            ));
        }

        if (AuthUtil.hasRole("COMPANY")) {
            User user = AuthUtil.currentUser();
            if (user == null || user.getCompanyId() == null) {
                return Result.validationError("企业账号未绑定企业，无法查看申请数据");
            }
            return Result.success(applicationMapper.selectCompanyApplications(page, keyword, user.getCompanyId()));
        }

        return Result.forbidden("仅企业/教师/管理员可查看审核列表");
    }

    @GetMapping("/company/list")
    public Result<?> findListForCompany(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("COMPANY")) {
            return Result.forbidden("仅企业角色可查看本企业申请数据");
        }

        User user = AuthUtil.currentUser();
        if (user == null || user.getCompanyId() == null) {
            return Result.validationError("企业账号未绑定企业，无法查看申请数据");
        }

        Page<Application> page = new Page<>(pageNum, pageSize);
        return Result.success(applicationMapper.selectCompanyApplications(page, keyword, user.getCompanyId()));
    }

    @PutMapping("/review")
    public Result<?> review(@RequestBody Application app) {
        if (!AuthUtil.hasRole("COMPANY", "TEACHER", "ADMIN")) {
            return Result.forbidden("仅企业/教师/管理员可审核申请");
        }

        if (app.getId() == null) {
            return Result.validationError("申请ID不能为空");
        }

        // 参数 app 里包含：id, status, remark
        if (app.getStatus() == null) {
            return Result.validationError("审核状态不能为空");
        }
        if (app.getRemark() != null && app.getRemark().length() > 255) {
            return Result.validationError("审核意见不能超过255个字符");
        }

        Application dbApp = applicationMapper.selectById(app.getId());
        if (dbApp == null) {
            return Result.notFound("申请记录不存在");
        }

        boolean companyReview = AuthUtil.hasRole("COMPANY") && !AuthUtil.hasRole("ADMIN") && !AuthUtil.hasRole("TEACHER");
        boolean teacherReview = AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN");
        int targetStatus = app.getStatus();
        String stage;
        Long reviewTeacherIdToSet = null;

        if (companyReview) {
            User currentUser = AuthUtil.currentUser();
            if (currentUser == null || currentUser.getCompanyId() == null) {
                return Result.validationError("企业账号未绑定企业，无法审核申请");
            }

            Position position = positionMapper.selectById(dbApp.getPositionId());
            if (position == null || position.getCompanyId() == null || !position.getCompanyId().equals(currentUser.getCompanyId())) {
                return Result.forbidden("仅可预审本企业岗位申请");
            }

            if (dbApp.getStatus() == null || dbApp.getStatus() != STATUS_PENDING_COMPANY_REVIEW) {
                return Result.validationError("当前状态不允许企业预审，需处于“待企业预审”");
            }
            if (targetStatus != STATUS_COMPANY_APPROVED && targetStatus != STATUS_REJECTED) {
                return Result.validationError("企业预审仅支持：通过(1)或驳回(3)");
            }

            if (targetStatus == STATUS_COMPANY_APPROVED) {
                if (dbApp.getReviewTeacherId() != null) {
                    reviewTeacherIdToSet = dbApp.getReviewTeacherId();
                } else {
                    User student = userMapper.selectById(dbApp.getStudentId());
                    if (student == null || student.getTeacherId() == null) {
                        return Result.validationError("学生未绑定负责教师，请联系管理员在用户管理中修正后再通过");
                    }
                    User reviewTeacher = userMapper.selectById(student.getTeacherId());
                    if (reviewTeacher == null || !"TEACHER".equalsIgnoreCase(reviewTeacher.getRole())) {
                        return Result.validationError("学生绑定的负责教师无效，请联系管理员修正后再通过");
                    }
                    reviewTeacherIdToSet = student.getTeacherId();
                }
            }
            stage = "企业预审";
        } else if (teacherReview) {
            if (dbApp.getStatus() == null || dbApp.getStatus() != STATUS_COMPANY_APPROVED) {
                return Result.validationError("当前状态不允许教师审核，需处于“企业预审通过”");
            }
            if (targetStatus != STATUS_TEACHER_APPROVED && targetStatus != STATUS_REJECTED) {
                return Result.validationError("教师审核仅支持：通过(2)或驳回(3)");
            }

            if (dbApp.getReviewTeacherId() == null) {
                return Result.validationError("该申请未指派审核教师，请联系管理员指派后再审核");
            }
            if (!dbApp.getReviewTeacherId().equals(AuthUtil.currentUserId())) {
                return Result.forbidden("该申请已指派给其他教师审核");
            }
            stage = "教师初审";
        } else {
            if (dbApp.getStatus() == null || dbApp.getStatus() != STATUS_TEACHER_APPROVED) {
                if (dbApp.getStatus() != null && dbApp.getStatus() == STATUS_ASSIGNED) {
                    return Result.validationError("申请已完成分配，不能重复终审");
                }
                return Result.validationError("当前状态不允许管理员终审，需处于“教师已通过”");
            }
            if (targetStatus != STATUS_ADMIN_APPROVED && targetStatus != STATUS_REJECTED) {
                return Result.validationError("管理员终审仅支持：通过(4)或驳回(3)");
            }
            if (targetStatus == STATUS_ADMIN_APPROVED
                    && assignmentMapper.countActiveByStudentId(dbApp.getStudentId()) > 0) {
                return Result.conflict("该学生已有进行中实习，当前申请不能进入待分配");
            }
            stage = "管理员终审";
        }

        // 如果管理员通过，自动创建实习分配（跳过手动分配步骤）
        if (!companyReview && !teacherReview && targetStatus == STATUS_ADMIN_APPROVED) {
            Position position = positionMapper.selectById(dbApp.getPositionId());
            if (position == null) {
                return Result.error("对应岗位不存在，无法自动分配");
            }

            // 校内指导教师：沿用学生当前的 teacherId
            User student = userMapper.selectById(dbApp.getStudentId());
            Long teacherId = (student != null && student.getTeacherId() != null)
                    ? student.getTeacherId()
                    : (dbApp.getReviewTeacherId() != null ? dbApp.getReviewTeacherId() : null);

            if (teacherId == null) {
                return Result.error("无法确定指导教师，请先为该学生分配校内指导教师");
            }

            // 确定企业ID
            Long companyId = position.getCompanyId();
            if (companyId == null && position.getCompanyName() != null) {
                QueryWrapper<Company> companyQuery = new QueryWrapper<>();
                companyQuery.eq("name", position.getCompanyName());
                companyQuery.last("LIMIT 1");
                Company company = companyMapper.selectOne(companyQuery);
                if (company != null) {
                    companyId = company.getId();
                }
            }

            // 企业导师：负载均衡自动分配（选择当前带学生最少的企业导师）
            Long mentorId = null;
            if (companyId != null) {
                mentorId = userMapper.selectLeastLoadedMentorByCompanyId(companyId);
            }

            // 创建 Assignment
            Assignment assignment = new Assignment();
            assignment.setApplicationId(dbApp.getId());
            assignment.setStudentId(dbApp.getStudentId());
            assignment.setPositionId(dbApp.getPositionId());
            assignment.setTeacherId(teacherId);
            assignment.setMentorId(mentorId);
            assignment.setCompanyId(companyId);
            assignment.setStatus(1);
            assignment.setRemark("管理员终审通过自动分配");
            assignment.setAssignTime(LocalDateTime.now());
            assignment.setUpdateTime(LocalDateTime.now());
            assignmentMapper.insert(assignment);

            // 同步更新学生的 teacherId
            if (student != null && !teacherId.equals(student.getTeacherId())) {
                User studentUpdate = new User();
                studentUpdate.setId(dbApp.getStudentId());
                studentUpdate.setTeacherId(teacherId);
                userMapper.updateById(studentUpdate);
            }

            applicationMapper.updateReviewTeacherForPendingByStudentId(dbApp.getStudentId(), teacherId);

            // 直接设为已分配状态
            targetStatus = STATUS_ASSIGNED;

            User teacherUser = userMapper.selectById(teacherId);
            internshipRecordService.addRecord(
                    dbApp.getStudentId(),
                    "ASSIGNMENT",
                    "实习分配已自动完成，指导老师：" + (teacherUser != null ? teacherUser.getName() : teacherId) +
                            (mentorId != null ? "，企业导师已自动分配" : "，暂无企业导师"),
                    assignment.getId()
            );

            auditLogService.record(
                    "ASSIGNMENT_AUTO_CREATE",
                    "ASSIGNMENT",
                    assignment.getId(),
                    "管理员终审自动分配，申请ID=" + dbApp.getId() +
                            "，学生ID=" + dbApp.getStudentId() +
                            "，教师ID=" + teacherId +
                            "，企业导师ID=" + (mentorId == null ? "-" : mentorId)
            );
        }


        Application update = new Application();
        update.setId(app.getId());
        update.setStatus(targetStatus);
        update.setRemark(app.getRemark());
        if (reviewTeacherIdToSet != null) {
            update.setReviewTeacherId(reviewTeacherIdToSet);
        }
        applicationMapper.updateById(update);

        String eventType;
        String eventDetail;
        if (companyReview) {
            eventType = targetStatus == STATUS_COMPANY_APPROVED ? "APPLICATION_APPROVE" : "APPLICATION_REJECT";
            eventDetail = targetStatus == STATUS_COMPANY_APPROVED
                    ? "企业预审通过，待教师初审"
                    : "企业预审驳回";
        } else if (teacherReview) {
            eventType = targetStatus == STATUS_TEACHER_APPROVED ? "APPLICATION_APPROVE" : "APPLICATION_REJECT";
            eventDetail = targetStatus == STATUS_TEACHER_APPROVED
                    ? "教师初审通过，待管理员终审"
                    : "教师初审驳回";
        } else {
            eventType = targetStatus == STATUS_ASSIGNED ? "APPLICATION_APPROVE" : "APPLICATION_REJECT";
            eventDetail = targetStatus == STATUS_ASSIGNED
                    ? "管理员终审通过，已自动完成实习分配"
                    : "管理员终审驳回";
        }

        internshipRecordService.addRecord(
                dbApp.getStudentId(),
                eventType,
                eventDetail + (StringUtils.hasText(app.getRemark()) ? "，意见：" + app.getRemark() : ""),
                dbApp.getId()
        );

        String resultText = targetStatus == STATUS_REJECTED ? "驳回" : "通过";
        auditLogService.record(
                "APPLICATION_REVIEW",
                "APPLICATION",
                dbApp.getId(),
                stage + "，结果：" + resultText + "，学生ID=" + dbApp.getStudentId() +
                        (app.getRemark() == null || app.getRemark().isBlank() ? "" : "，意见=" + app.getRemark())
        );

        return Result.success(null);
    }

    private Result<?> audit(ApplicationAuditRequest app) {
        Application application = new Application();
        application.setId(app.getId());
        application.setStatus(app.getStatus());
        application.setRemark(app.getReviewRemark());
        return review(application);
    }

    @PutMapping("/assign-teacher")
    public Result<?> assignTeacher(@RequestBody AssignTeacherRequest request) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可指派审核教师");
        }

        if (request == null || request.getId() == null) {
            return Result.validationError("申请ID不能为空");
        }
        if (request.getReviewTeacherId() == null) {
            return Result.validationError("审核教师不能为空");
        }

        Application dbApp = applicationMapper.selectById(request.getId());
        if (dbApp == null) {
            return Result.notFound("申请记录不存在");
        }
        if (dbApp.getStatus() == null || dbApp.getStatus() != STATUS_COMPANY_APPROVED) {
            return Result.validationError("仅“待教师审核”阶段可指派审核教师");
        }

        User teacher = userMapper.selectById(request.getReviewTeacherId());
        if (teacher == null || !"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            return Result.validationError("审核教师无效");
        }

        Application update = new Application();
        update.setId(dbApp.getId());
        update.setReviewTeacherId(request.getReviewTeacherId());
        applicationMapper.updateById(update);

        User studentUpdate = new User();
        studentUpdate.setId(dbApp.getStudentId());
        studentUpdate.setTeacherId(request.getReviewTeacherId());
        userMapper.updateById(studentUpdate);

        applicationMapper.updateReviewTeacherForPendingByStudentId(dbApp.getStudentId(), request.getReviewTeacherId());

        internshipRecordService.addRecord(
                dbApp.getStudentId(),
                "APPLICATION_ASSIGN_TEACHER",
                "管理员指派审核教师：" + (StringUtils.hasText(teacher.getName()) ? teacher.getName() : teacher.getUsername()),
                dbApp.getId()
        );

        auditLogService.record(
                "APPLICATION_ASSIGN_TEACHER",
                "APPLICATION",
                dbApp.getId(),
                "审核教师ID=" + request.getReviewTeacherId() +
                        (StringUtils.hasText(request.getRemark()) ? "，备注=" + request.getRemark() : "")
        );

        return Result.success(null);
    }

    @Data
    public static class AssignTeacherRequest {
        private Long id;
        private Long reviewTeacherId;
        private String remark;
    }

    @Data
    public static class ApplicationAuditRequest {
        private Long id;
        private Integer status;
        private String reviewRemark;
    }

    @Data
    public static class BatchAuditRequest {
        private List<Long> ids;
        private Integer status;
        private String reviewRemark;
    }
}
