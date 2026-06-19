/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.AuditLog;
import fun.bmoqing.internship.mapper.AuditLogMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    public AuditLogController(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword,
                          @RequestParam(defaultValue = "") String action,
                          @RequestParam(defaultValue = "") String targetType) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可查看审计日志");
        }

        Page<AuditLog> page = new Page<>(pageNum, pageSize);
        return Result.success(auditLogMapper.selectPageForAdmin(page, keyword, action, targetType));
    }
}
