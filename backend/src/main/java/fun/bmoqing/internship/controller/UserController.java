/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.PasswordPolicyUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Company;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.ApplicationMapper;
import fun.bmoqing.internship.mapper.CompanyMapper;
import fun.bmoqing.internship.mapper.UserMapper;
import fun.bmoqing.internship.service.AuditLogService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ApplicationMapper applicationMapper;

    // 1. 分页查询
    // GET /api/user?pageNum=1&pageSize=10&username=张三
    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String username) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可访问用户管理");
        }

        // 创建分页对象
        Page<User> page = new Page<>(pageNum, pageSize);
        // 创建查询条件
        QueryWrapper<User> query = new QueryWrapper<>();
        if (StringUtils.hasText(username)) {
            query.like("username", username); // 模糊查询
        }
        query.orderByDesc("id"); // 按ID倒序排

        // 执行查询
        Page<User> userPage = userMapper.selectPage(page, query);
        userPage.getRecords().forEach(item -> item.setPassword(null));
        return Result.success(userPage);
    }

    // 2. 新增用户
    @PostMapping
    public Result<?> save(@RequestBody User user) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可新增用户");
        }

        if (!StringUtils.hasText(user.getUsername())) {
            return Result.validationError("用户名不能为空");
        }
        if (!StringUtils.hasText(user.getRole())) {
            return Result.validationError("角色不能为空");
        }
        if (!StringUtils.hasText(user.getName())) {
            return Result.validationError("姓名不能为空");
        }
        if (user.getUsername().length() < 4 || user.getUsername().length() > 20) {
            return Result.validationError("用户名长度需在4-20位之间");
        }
        user.setRole(user.getRole().toUpperCase());
        if (!isValidRole(user.getRole())) {
            return Result.validationError("角色无效");
        }

        QueryWrapper<User> existsQuery = new QueryWrapper<>();
        existsQuery.eq("username", user.getUsername());
        if (userMapper.selectCount(existsQuery) > 0) {
            return Result.conflict("用户名已存在");
        }

        if ("COMPANY".equalsIgnoreCase(user.getRole())) {
            if (user.getCompanyId() == null) {
                return Result.validationError("企业账号必须绑定企业ID");
            }
            Company company = companyMapper.selectById(user.getCompanyId());
            if (company == null) {
                return Result.notFound("绑定企业不存在");
            }
            user.setTeacherId(null);
        } else if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            user.setCompanyId(null);
            user.setTeacherId(null);
        } else if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            user.setCompanyId(null);
            if (user.getTeacherId() == null) {
                return Result.validationError("学生账号必须绑定负责教师");
            }
            if (!isValidTeacher(user.getTeacherId())) {
                return Result.validationError("负责教师无效");
            }
        } else {
            user.setCompanyId(null);
            user.setTeacherId(null);
        }

        if (!StringUtils.hasText(user.getPassword())) {
            return Result.validationError("新增用户必须设置初始密码");
        }

        String passwordPolicyError = PasswordPolicyUtil.validate(user.getPassword());
        if (passwordPolicyError != null) {
            return Result.validationError(passwordPolicyError);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);

        auditLogService.record(
                "USER_CREATE",
                "USER",
                user.getId(),
                "用户名=" + user.getUsername() + "，角色=" + user.getRole() +
                        (user.getCompanyId() == null ? "" : "，企业ID=" + user.getCompanyId())
        );
        return Result.success(null);
    }

    // 3. 修改用户
    @PutMapping
    public Result<?> update(@RequestBody User user) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可修改用户");
        }

        if (user.getId() == null) {
            return Result.validationError("用户ID不能为空");
        }
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            return Result.notFound("用户不存在");
        }

        if (StringUtils.hasText(user.getRole()) && !isValidRole(user.getRole())) {
            return Result.validationError("角色无效");
        }

        if (StringUtils.hasText(user.getRole())) {
            user.setRole(user.getRole().toUpperCase());
        }

        String targetRole = StringUtils.hasText(user.getRole()) ? user.getRole() : dbUser.getRole();
        if ("COMPANY".equalsIgnoreCase(targetRole)) {
            if (user.getCompanyId() == null) {
                return Result.validationError("企业账号必须绑定企业ID");
            }
            Company company = companyMapper.selectById(user.getCompanyId());
            if (company == null) {
                return Result.notFound("绑定企业不存在");
            }
            user.setTeacherId(null);
        } else if ("TEACHER".equalsIgnoreCase(targetRole)) {
            user.setCompanyId(null);
            user.setTeacherId(null);
        } else if ("STUDENT".equalsIgnoreCase(targetRole)) {
            user.setCompanyId(null);
            Long targetTeacherId = user.getTeacherId() == null ? dbUser.getTeacherId() : user.getTeacherId();
            if (targetTeacherId == null) {
                return Result.validationError("学生账号必须绑定负责教师");
            }
            if (!isValidTeacher(targetTeacherId)) {
                return Result.validationError("负责教师无效");
            }
        } else if (StringUtils.hasText(user.getRole())) {
            user.setCompanyId(null);
            user.setTeacherId(null);
        }

        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(null);
        } else {
            String passwordPolicyError = PasswordPolicyUtil.validate(user.getPassword());
            if (passwordPolicyError != null) {
                return Result.validationError(passwordPolicyError);
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        Long oldTeacherId = dbUser.getTeacherId();
        Long newTeacherId = user.getTeacherId() == null ? dbUser.getTeacherId() : user.getTeacherId();
        userMapper.updateById(user);

        if ("STUDENT".equalsIgnoreCase(targetRole)
                && newTeacherId != null
                && !newTeacherId.equals(oldTeacherId)) {
            applicationMapper.updateReviewTeacherForPendingByStudentId(dbUser.getId(), newTeacherId);
        }

        auditLogService.record(
                "USER_UPDATE",
                "USER",
                dbUser.getId(),
                "更新用户信息，用户名=" + dbUser.getUsername() +
                        (StringUtils.hasText(user.getRole()) ? "，新角色=" + user.getRole() : "")
        );
        return Result.success(null);
    }

    @PutMapping("/resume")
    public Result<?> updateResume(@RequestBody User user) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以上传更新简历");
        }
        if (!StringUtils.hasText(user.getResumeUrl())) {
            return Result.validationError("简历链接不能为空");
        }

        User update = new User();
        update.setId(AuthUtil.currentUserId());
        update.setResumeUrl(user.getResumeUrl());
        userMapper.updateById(update);

        auditLogService.record(
                "RESUME_UPDATE",
                "USER",
                AuthUtil.currentUserId(),
                "学生更新个人简历"
        );
        return Result.success(null);
    }

    @Data
    static class ProfileUpdateRequest {
        private String name;
        private String oldPassword;
        private String newPassword;
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody ProfileUpdateRequest request) {
        Long userId = AuthUtil.currentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            return Result.notFound("用户不存在");
        }

        boolean nameChanged = false;
        boolean passwordChanged = false;

        // 修改姓名
        if (StringUtils.hasText(request.getName())) {
            String trimmedName = request.getName().trim();
            if (trimmedName.length() > 30) {
                return Result.validationError("姓名不能超过30个字符");
            }
            if (!trimmedName.equals(dbUser.getName())) {
                nameChanged = true;
            }
        }

        // 修改密码
        if (StringUtils.hasText(request.getNewPassword())) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                return Result.validationError("修改密码需提供原密码");
            }
            boolean oldMatch;
            try {
                oldMatch = passwordEncoder.matches(request.getOldPassword(), dbUser.getPassword());
            } catch (Exception e) {
                oldMatch = false;
            }
            if (!oldMatch) {
                return Result.validationError("原密码错误");
            }
            String passwordPolicyError = PasswordPolicyUtil.validate(request.getNewPassword());
            if (passwordPolicyError != null) {
                return Result.validationError(passwordPolicyError);
            }
            passwordChanged = true;
        }

        if (!nameChanged && !passwordChanged) {
            return Result.validationError("没有需要修改的内容");
        }

        User update = new User();
        update.setId(userId);
        if (nameChanged) {
            update.setName(request.getName().trim());
        }
        if (passwordChanged) {
            update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        userMapper.updateById(update);

        auditLogService.record(
                "PROFILE_UPDATE",
                "USER",
                userId,
                "用户自助修改个人信息" +
                        (nameChanged ? "，修改姓名" : "") +
                        (passwordChanged ? "，修改密码" : "")
        );

        // 返回更新后的用户信息
        User updatedUser = userMapper.selectById(userId);
        updatedUser.setPassword(null);
        return Result.success(updatedUser);
    }

    @GetMapping("/managed-students")
    public Result<?> managedStudents(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("TEACHER")) {
            return Result.forbidden("仅教师可查看管理学生列表");
        }

        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> result = userMapper.selectManagedStudents(page, AuthUtil.currentUserId(), keyword);
        result.getRecords().forEach(item -> item.setPassword(null));
        return Result.success(result);
    }

    // 4. 删除用户
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可删除用户");
        }

        if (AuthUtil.currentUserId() != null && AuthUtil.currentUserId().equals(id)) {
            return Result.error("不能删除当前登录账号");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.notFound("用户不存在");
        }

        userMapper.deleteById(id);

        auditLogService.record(
                "USER_DELETE",
                "USER",
                id,
                "删除用户，用户名=" + user.getUsername() + "，角色=" + user.getRole()
        );
        return Result.success(null);
    }

    private boolean isValidRole(String role) {
        return "ADMIN".equalsIgnoreCase(role)
                || "TEACHER".equalsIgnoreCase(role)
                || "STUDENT".equalsIgnoreCase(role)
                || "COMPANY".equalsIgnoreCase(role);
    }

    private boolean isValidTeacher(Long teacherId) {
        if (teacherId == null) {
            return false;
        }
        User teacher = userMapper.selectById(teacherId);
        return teacher != null && "TEACHER".equalsIgnoreCase(teacher.getRole());
    }
}
