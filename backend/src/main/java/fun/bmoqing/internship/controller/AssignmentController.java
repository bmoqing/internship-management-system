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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/assignment")
public class AssignmentController {

    private static final int APPLICATION_STATUS_ADMIN_APPROVED = 4;
    private static final int APPLICATION_STATUS_ASSIGNED = 5;

    private final AssignmentMapper assignmentMapper;
    private final ApplicationMapper applicationMapper;
    private final PositionMapper positionMapper;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final InternshipRecordService internshipRecordService;
    private final AuditLogService auditLogService;

    public AssignmentController(AssignmentMapper assignmentMapper,
                                ApplicationMapper applicationMapper,
                                PositionMapper positionMapper,
                                UserMapper userMapper,
                                CompanyMapper companyMapper,
                                InternshipRecordService internshipRecordService,
                                AuditLogService auditLogService) {
        this.assignmentMapper = assignmentMapper;
        this.applicationMapper = applicationMapper;
        this.positionMapper = positionMapper;
        this.userMapper = userMapper;
        this.companyMapper = companyMapper;
        this.internshipRecordService = internshipRecordService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword) {
        Page<Assignment> page = new Page<>(pageNum, pageSize);

        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(assignmentMapper.selectPageForAdmin(page, keyword));
        }
        if (AuthUtil.hasRole("TEACHER")) {
            return Result.success(assignmentMapper.selectPageForTeacher(page, keyword, AuthUtil.currentUserId()));
        }
        if (AuthUtil.hasRole("COMPANY")) {
            User currentUser = AuthUtil.currentUser();
            if (currentUser != null && currentUser.getCompanyId() != null) {
                return Result.success(assignmentMapper.selectPageForCompany(page, keyword, currentUser.getCompanyId()));
            }
            return Result.success(assignmentMapper.selectPageForMentor(page, keyword, AuthUtil.currentUserId()));
        }
        if (AuthUtil.hasRole("STUDENT")) {
            return Result.success(assignmentMapper.selectPageForStudent(page, AuthUtil.currentUserId()));
        }

        return Result.forbidden("无权限访问分配列表");
    }

    @GetMapping("/candidates")
    public Result<?> candidates(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可查看待分配申请");
        }

        Page<Application> page = new Page<>(pageNum, pageSize);
        return Result.success(applicationMapper.selectApprovedWithoutAssignment(page, keyword));
    }

    @GetMapping("/teachers")
    public Result<?> teachers() {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可查看指导老师列表");
        }

        List<User> teachers = userMapper.selectByRole("TEACHER");
        teachers.forEach(item -> item.setPassword(null));
        return Result.success(teachers);
    }

    @GetMapping("/mentors")
    public Result<?> mentors(@RequestParam(required = false) Long companyId) {
        List<User> mentors;
        if (AuthUtil.hasRole("ADMIN")) {
            if (companyId != null) {
                mentors = userMapper.selectCompanyUsersByCompanyId(companyId);
            } else {
                mentors = userMapper.selectByRole("COMPANY");
            }
        } else if (AuthUtil.hasRole("COMPANY")) {
            User currentUser = AuthUtil.currentUser();
            if (currentUser == null || currentUser.getCompanyId() == null) {
                return Result.validationError("企业账号未绑定企业，无法查看企业导师");
            }
            mentors = userMapper.selectCompanyUsersByCompanyId(currentUser.getCompanyId());
        } else {
            return Result.forbidden("仅管理员/企业可查看企业导师列表");
        }
        mentors.forEach(item -> item.setPassword(null));
        return Result.success(mentors);
    }

    @PutMapping("/mentor")
    public Result<?> assignMentor(@RequestBody MentorAssignRequest request) {
        if (!AuthUtil.hasRole("ADMIN", "COMPANY")) {
            return Result.forbidden("仅管理员/企业可分配企业导师");
        }

        if (request.getAssignmentId() == null) {
            return Result.validationError("分配ID不能为空");
        }

        Assignment assignment = assignmentMapper.selectById(request.getAssignmentId());
        if (assignment == null) {
            return Result.notFound("分配记录不存在");
        }

        if (assignment.getStatus() == null || assignment.getStatus() != 1) {
            return Result.validationError("仅可调整进行中的分配记录");
        }

        if (AuthUtil.hasRole("COMPANY")) {
            User currentUser = AuthUtil.currentUser();
            if (currentUser == null || currentUser.getCompanyId() == null) {
                return Result.validationError("企业账号未绑定企业，无法分配企业导师");
            }
            if (assignment.getCompanyId() == null || !assignment.getCompanyId().equals(currentUser.getCompanyId())) {
                return Result.forbidden("仅可管理本企业学生的企业导师");
            }
        }

        if (request.getMentorId() != null) {
            User mentor = userMapper.selectById(request.getMentorId());
            if (mentor == null || !"COMPANY".equalsIgnoreCase(mentor.getRole())) {
                return Result.validationError("企业导师无效");
            }
            if (assignment.getCompanyId() != null
                    && mentor.getCompanyId() != null
                    && !assignment.getCompanyId().equals(mentor.getCompanyId())) {
                return Result.validationError("企业导师不属于该分配所属企业");
            }
        }

        Assignment update = new Assignment();
        update.setId(assignment.getId());
        update.setMentorId(request.getMentorId());
        if (request.getRemark() != null && !request.getRemark().isBlank()) {
            update.setRemark(request.getRemark());
        }
        update.setUpdateTime(LocalDateTime.now());
        assignmentMapper.updateById(update);

        internshipRecordService.addRecord(
                assignment.getStudentId(),
                "ASSIGNMENT_MENTOR_UPDATE",
                request.getMentorId() == null ? "企业导师已清空" : "企业导师已调整",
                assignment.getId()
        );

        auditLogService.record(
                "ASSIGNMENT_MENTOR_UPDATE",
                "ASSIGNMENT",
                assignment.getId(),
                "企业导师ID=" + (request.getMentorId() == null ? "-" : request.getMentorId()) +
                        (request.getRemark() == null || request.getRemark().isBlank() ? "" : "，备注=" + request.getRemark())
        );

        return Result.success(null);
    }

    @PostMapping("/assign")
    public Result<?> assign(@RequestBody AssignRequest request) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可执行分配");
        }

        if (request.getApplicationId() == null) {
            return Result.validationError("申请ID不能为空");
        }

        Application application = applicationMapper.selectById(request.getApplicationId());
        if (application == null) {
            return Result.notFound("申请记录不存在");
        }
        if (application.getStatus() == null || application.getStatus() != APPLICATION_STATUS_ADMIN_APPROVED) {
            return Result.validationError("仅管理员终审通过（状态4）的申请可进行分配");
        }

        if (assignmentMapper.countActiveByApplicationId(request.getApplicationId()) > 0) {
            return Result.conflict("该申请已完成分配，请勿重复操作");
        }

        if (assignmentMapper.countActiveByStudentId(application.getStudentId()) > 0) {
            return Result.conflict("该学生已有进行中的实习分配，不能重复分配");
        }

        Position position = positionMapper.selectById(application.getPositionId());
        if (position == null) {
            return Result.notFound("对应岗位不存在");
        }

        Long teacherId = request.getTeacherId();
        if (teacherId == null) {
            return Result.validationError("请指定指导教师");
        }

        User teacher = userMapper.selectById(teacherId);
        if (teacher == null || !"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            return Result.validationError("指导教师无效");
        }

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

        Long mentorId = request.getMentorId();
        if (mentorId != null) {
            User mentor = userMapper.selectById(mentorId);
            if (mentor == null || !"COMPANY".equalsIgnoreCase(mentor.getRole())) {
                return Result.validationError("企业导师无效");
            }
            if (companyId != null && mentor.getCompanyId() != null && !companyId.equals(mentor.getCompanyId())) {
                return Result.validationError("企业导师不属于该岗位所属企业");
            }
        }

        Assignment assignment = new Assignment();
        assignment.setApplicationId(application.getId());
        assignment.setStudentId(application.getStudentId());
        assignment.setPositionId(application.getPositionId());
        assignment.setTeacherId(teacherId);
        assignment.setMentorId(mentorId);
        assignment.setCompanyId(companyId);
        assignment.setStatus(1);
        assignment.setRemark(request.getRemark());
        assignment.setAssignTime(LocalDateTime.now());
        assignment.setUpdateTime(LocalDateTime.now());
        assignmentMapper.insert(assignment);

        Application update = new Application();
        update.setId(application.getId());
        update.setStatus(APPLICATION_STATUS_ASSIGNED);
        if (request.getRemark() != null && !request.getRemark().isBlank()) {
            update.setRemark(request.getRemark());
        }
        applicationMapper.updateById(update);

        User studentUpdate = new User();
        studentUpdate.setId(application.getStudentId());
        studentUpdate.setTeacherId(teacherId);
        userMapper.updateById(studentUpdate);

        applicationMapper.updateReviewTeacherForPendingByStudentId(application.getStudentId(), teacherId);

        internshipRecordService.addRecord(
                application.getStudentId(),
                "ASSIGNMENT",
                "实习分配已完成，指导老师：" + teacher.getName(),
                assignment.getId()
        );

        auditLogService.record(
                "ASSIGNMENT_CREATE",
                "ASSIGNMENT",
                assignment.getId(),
                "申请ID=" + application.getId() +
                        "，学生ID=" + application.getStudentId() +
                        "，教师ID=" + teacherId +
                        "，企业导师ID=" + (mentorId == null ? "-" : mentorId)
        );

        return Result.success(assignment);
    }

    @Data
    public static class AssignRequest {
        private Long applicationId;
        private Long teacherId;
        private Long mentorId;
        private String remark;
    }

    @Data
    public static class MentorAssignRequest {
        private Long assignmentId;
        private Long mentorId;
        private String remark;
    }
}
