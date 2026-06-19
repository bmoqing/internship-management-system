/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.SysConfig;
import fun.bmoqing.internship.mapper.SysConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class SysConfigController {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @GetMapping("/list")
    public Result<?> getConfigs() {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可查看系统配置");
        }
        return Result.success(sysConfigMapper.selectList(new QueryWrapper<>()));
    }

    @PutMapping("/batch")
    public Result<?> updateConfigs(@RequestBody List<SysConfig> configs) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可修改系统配置");
        }
        
        for (SysConfig config : configs) {
            if (config.getId() != null) {
                SysConfig update = new SysConfig();
                update.setId(config.getId());
                update.setConfigValue(config.getConfigValue());
                sysConfigMapper.updateById(update);
            }
        }
        return Result.success(null);
    }

    @GetMapping("/weights")
    public Result<?> getWeights() {
        QueryWrapper<SysConfig> query = new QueryWrapper<>();
        query.likeRight("config_key", "score.weight.");
        return Result.success(sysConfigMapper.selectList(query));
    }
}
