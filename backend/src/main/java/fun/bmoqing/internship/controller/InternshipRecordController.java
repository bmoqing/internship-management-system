/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.InternshipRecord;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.InternshipRecordMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/record")
public class InternshipRecordController {

    private final InternshipRecordMapper internshipRecordMapper;

    public InternshipRecordController(InternshipRecordMapper internshipRecordMapper) {
        this.internshipRecordMapper = internshipRecordMapper;
    }

    @GetMapping("/my")
    public Result<?> myRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以查看个人实习记录");
        }

        Page<InternshipRecord> page = new Page<>(pageNum, pageSize);
        return Result.success(internshipRecordMapper.selectPageForStudent(page, AuthUtil.currentUserId()));
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword) {
        Page<InternshipRecord> page = new Page<>(pageNum, pageSize);

        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(internshipRecordMapper.selectPageForAdmin(page, keyword));
        }
        if (AuthUtil.hasRole("TEACHER")) {
            return Result.success(internshipRecordMapper.selectPageForTeacher(page, keyword, AuthUtil.currentUserId()));
        }
        if (AuthUtil.hasRole("COMPANY")) {
            User user = AuthUtil.currentUser();
            if (user == null || user.getCompanyId() == null) {
                return Result.error("企业账号尚未绑定企业，无法查看记录");
            }
            return Result.success(internshipRecordMapper.selectPageForCompany(page, keyword, user.getCompanyId()));
        }

        return Result.forbidden("无权限访问实习记录列表");
    }
}
