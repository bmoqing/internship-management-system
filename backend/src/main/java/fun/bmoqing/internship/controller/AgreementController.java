package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Agreement;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.AgreementMapper;
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

@RestController
@RequestMapping("/api/agreement")
public class AgreementController {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_REVOKED = 3; // 打回待修改（不影响实习状态）

    private final AgreementMapper agreementMapper;
    private final AssignmentMapper assignmentMapper;
    private final InternshipRecordService internshipRecordService;
    private final AuditLogService auditLogService;

    public AgreementController(AgreementMapper agreementMapper,
                               AssignmentMapper assignmentMapper,
                               InternshipRecordService internshipRecordService,
                               AuditLogService auditLogService) {
        this.agreementMapper = agreementMapper;
        this.assignmentMapper = assignmentMapper;
        this.internshipRecordService = internshipRecordService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword) {
        Page<Agreement> page = new Page<>(pageNum, pageSize);

        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(agreementMapper.selectPageForAdmin(page, keyword));
        }
        if (AuthUtil.hasRole("TEACHER")) {
            return Result.success(agreementMapper.selectPageForTeacher(page, keyword, AuthUtil.currentUserId()));
        }
        if (AuthUtil.hasRole("COMPANY")) {
            User user = AuthUtil.currentUser();
            if (user == null || user.getCompanyId() == null) {
                return Result.validationError("企业账号未绑定企业，无法查看协议");
            }
            return Result.success(agreementMapper.selectPageForCompany(page, keyword, user.getCompanyId()));
        }
        if (AuthUtil.hasRole("STUDENT")) {
            return Result.success(agreementMapper.selectPageForStudent(page, AuthUtil.currentUserId()));
        }

        return Result.forbidden("无权限访问协议列表");
    }

    @PostMapping("/upload")
    public Result<?> upload(@RequestBody Agreement agreement) {
        if (!AuthUtil.hasRole("STUDENT", "COMPANY")) {
            return Result.forbidden("仅学生/企业可上传协议");
        }

        if (agreement.getAssignmentId() == null) {
            return Result.validationError("分配ID不能为空");
        }
        if (!StringUtils.hasText(agreement.getTitle())) {
            return Result.validationError("协议标题不能为空");
        }
        if (!StringUtils.hasText(agreement.getContractUrl())) {
            return Result.validationError("协议链接不能为空");
        }
        if (agreement.getTitle().length() > 120) {
            return Result.validationError("协议标题不能超过120个字符");
        }
        if (agreement.getContractUrl().length() > 500) {
            return Result.validationError("协议链接不能超过500个字符");
        }
        if (agreement.getDescription() != null && agreement.getDescription().length() > 255) {
            return Result.validationError("协议说明不能超过255个字符");
        }

        Assignment assignment = assignmentMapper.selectById(agreement.getAssignmentId());
        if (assignment == null) {
            return Result.notFound("分配记录不存在");
        }
        if (assignment.getStatus() == null || assignment.getStatus() != 1) {
            return Result.validationError("仅可为进行中的分配上传协议");
        }

        User currentUser = AuthUtil.currentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Result.unauthorized("登录状态异常，请重新登录");
        }

        if (AuthUtil.hasRole("STUDENT") && !assignment.getStudentId().equals(currentUser.getId())) {
            return Result.forbidden("仅可上传自己的实习协议");
        }
        if (AuthUtil.hasRole("COMPANY")) {
            if (currentUser.getCompanyId() == null) {
                return Result.validationError("企业账号未绑定企业，无法上传协议");
            }
            if (assignment.getCompanyId() == null || !assignment.getCompanyId().equals(currentUser.getCompanyId())) {
                return Result.forbidden("仅可上传本企业相关实习协议");
            }
        }

        agreement.setStudentId(assignment.getStudentId());
        agreement.setCompanyId(assignment.getCompanyId());
        agreement.setTeacherId(assignment.getTeacherId());
        agreement.setUploaderId(currentUser.getId());
        agreement.setReviewerId(null);
        agreement.setReviewRemark(null);
        agreement.setStatus(STATUS_PENDING);
        agreement.setUploadTime(LocalDateTime.now());
        agreement.setUpdateTime(LocalDateTime.now());
        agreementMapper.insert(agreement);

        internshipRecordService.addRecord(
                assignment.getStudentId(),
                "AGREEMENT_UPLOAD",
                "上传实习协议：" + agreement.getTitle(),
                agreement.getId()
        );

        auditLogService.record(
                "AGREEMENT_UPLOAD",
                "AGREEMENT",
                agreement.getId(),
                "分配ID=" + agreement.getAssignmentId() +
                        "，学生ID=" + agreement.getStudentId() +
                        "，标题=" + agreement.getTitle()
        );

        return Result.success(agreement);
    }

    @PutMapping("/review")
    public Result<?> review(@RequestBody Agreement request) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师/管理员可审核协议");
        }

        if (request.getId() == null) {
            return Result.validationError("协议ID不能为空");
        }
        if (request.getStatus() == null) {
            return Result.validationError("审核状态不能为空");
        }
        if (request.getStatus() != STATUS_APPROVED && request.getStatus() != STATUS_REJECTED) {
            return Result.validationError("审核状态仅支持通过或驳回");
        }
        if (request.getReviewRemark() != null && request.getReviewRemark().length() > 255) {
            return Result.validationError("审核意见不能超过255个字符");
        }

        Agreement dbAgreement = agreementMapper.selectById(request.getId());
        if (dbAgreement == null) {
            return Result.notFound("协议记录不存在");
        }
        if (dbAgreement.getStatus() == null || dbAgreement.getStatus() != STATUS_PENDING) {
            return Result.validationError("仅待审核状态的协议可执行审核");
        }

        if (AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN")) {
            long relationCount = assignmentMapper.countTeacherStudentRelation(AuthUtil.currentUserId(), dbAgreement.getStudentId());
            if (relationCount <= 0) {
                return Result.forbidden("仅可审核自己指导学生的协议");
            }
        }

        Agreement update = new Agreement();
        update.setId(request.getId());
        update.setStatus(request.getStatus());
        update.setReviewerId(AuthUtil.currentUserId());
        update.setReviewRemark(request.getReviewRemark());
        update.setReviewTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        agreementMapper.updateById(update);

        boolean approved = request.getStatus() == STATUS_APPROVED;
        internshipRecordService.addRecord(
                dbAgreement.getStudentId(),
                approved ? "AGREEMENT_APPROVE" : "AGREEMENT_REJECT",
                approved ? "实习协议审核通过" : "实习协议审核驳回",
                dbAgreement.getId()
        );

        auditLogService.record(
                "AGREEMENT_REVIEW",
                "AGREEMENT",
                dbAgreement.getId(),
                "审核结果=" + (approved ? "通过" : "驳回") +
                        (request.getReviewRemark() == null || request.getReviewRemark().isBlank()
                                ? ""
                                : "，意见=" + request.getReviewRemark())
        );

        return Result.success(null);
    }

    @PutMapping("/revoke")
    public Result<?> revoke(@RequestBody Agreement request) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可打回已审核协议");
        }

        if (request.getId() == null) {
            return Result.validationError("协议ID不能为空");
        }
        if (request.getReviewRemark() != null && request.getReviewRemark().length() > 255) {
            return Result.validationError("打回原因不能超过255个字符");
        }

        Agreement dbAgreement = agreementMapper.selectById(request.getId());
        if (dbAgreement == null) {
            return Result.notFound("协议记录不存在");
        }
        if (dbAgreement.getStatus() == null ||
                (dbAgreement.getStatus() != STATUS_APPROVED && dbAgreement.getStatus() != STATUS_REJECTED)) {
            return Result.validationError("仅已通过或已驳回状态的协议可被打回");
        }

        Agreement update = new Agreement();
        update.setId(request.getId());
        update.setStatus(STATUS_REVOKED);
        update.setReviewerId(AuthUtil.currentUserId());
        update.setReviewRemark(request.getReviewRemark());
        update.setReviewTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        agreementMapper.updateById(update);

        internshipRecordService.addRecord(
                dbAgreement.getStudentId(),
                "AGREEMENT_REVOKE",
                "管理员打回已通过协议，请重新提交",
                dbAgreement.getId()
        );

        auditLogService.record(
                "AGREEMENT_REVOKE",
                "AGREEMENT",
                dbAgreement.getId(),
                "管理员打回已通过协议" +
                        (request.getReviewRemark() == null || request.getReviewRemark().isBlank()
                                ? ""
                                : "，原因=" + request.getReviewRemark())
        );

        return Result.success(null);
    }
}
