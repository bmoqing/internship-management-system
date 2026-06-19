package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Company;
import fun.bmoqing.internship.entity.Position;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.CompanyMapper;
import fun.bmoqing.internship.mapper.PositionMapper;
import fun.bmoqing.internship.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/position")
public class PositionController {

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private AuditLogService auditLogService;

    // 分页查询
    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String keyword) { // 既搜公司也搜岗位
        Page<Position> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Position> query = new QueryWrapper<>();

        if (AuthUtil.hasRole("COMPANY") && !AuthUtil.hasRole("ADMIN")) {
            User currentUser = AuthUtil.currentUser();
            if (currentUser == null || currentUser.getCompanyId() == null) {
                return Result.error("企业账号未绑定企业信息，请联系管理员");
            }
            query.eq("company_id", currentUser.getCompanyId());
        } else if (AuthUtil.hasRole("STUDENT")) {
            query.eq("audit_status", 1); // 学生只能看审核通过的岗位
        }

        // 如果有搜索词，就同时匹配 公司名 OR 岗位名
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like("company_name", keyword)
                    .or()
                    .like("title", keyword));
        }
        query.orderByDesc("create_time");

        return Result.success(positionMapper.selectPage(page, query));
    }

    // 新增
    @PostMapping
    public Result<?> save(@RequestBody Position position) {
        if (!AuthUtil.hasRole("ADMIN", "TEACHER", "COMPANY")) {
            return Result.forbidden("仅企业/教师/管理员可发布岗位");
        }

        User currentUser = AuthUtil.currentUser();
        if (AuthUtil.hasRole("COMPANY") && !AuthUtil.hasRole("ADMIN")) {
            if (currentUser == null || currentUser.getCompanyId() == null) {
                return Result.error("企业账号未绑定企业信息，请联系管理员");
            }
            Company company = companyMapper.selectById(currentUser.getCompanyId());
            if (company == null) {
                return Result.error("企业信息不存在，请联系管理员");
            }
            position.setCompanyId(company.getId());
            position.setCompanyName(company.getName());
            position.setOwnerId(currentUser.getId());
        } else if (position.getCompanyId() != null) {
            Company company = companyMapper.selectById(position.getCompanyId());
            if (company != null) {
                position.setCompanyName(company.getName());
            }
        }

        Result<?> validateResult = validatePosition(position, false);
        if (validateResult != null) {
            return validateResult;
        }

        position.setCreateTime(LocalDateTime.now());
        if (position.getStatus() == null) {
            position.setStatus(1);
        }
        if (AuthUtil.hasRole("ADMIN")) {
            position.setAuditStatus(1); // 管理员发布的默认通过
        } else {
            position.setAuditStatus(0); // 企业发布的默认待审核
        }
        positionMapper.insert(position);

        auditLogService.record(
                "POSITION_CREATE",
                "POSITION",
                position.getId(),
                "新增岗位，标题=" + position.getTitle() + "，企业=" + position.getCompanyName()
        );
        return Result.success(null);
    }

    // 修改
    @PutMapping
    public Result<?> update(@RequestBody Position position) {
        if (!AuthUtil.hasRole("ADMIN", "TEACHER", "COMPANY")) {
            return Result.forbidden("仅企业/教师/管理员可修改岗位");
        }

        Position dbPosition = positionMapper.selectById(position.getId());
        if (dbPosition == null) {
            return Result.error("岗位不存在");
        }

        if (AuthUtil.hasRole("COMPANY") && !AuthUtil.hasRole("ADMIN")) {
            User currentUser = AuthUtil.currentUser();
            if (currentUser == null || currentUser.getCompanyId() == null) {
                return Result.error("企业账号未绑定企业信息，请联系管理员");
            }
            if (!currentUser.getCompanyId().equals(dbPosition.getCompanyId())) {
                return Result.forbidden("仅可修改本企业岗位");
            }
        }

        if (AuthUtil.hasRole("COMPANY") && !AuthUtil.hasRole("ADMIN")) {
            position.setOwnerId(dbPosition.getOwnerId());
            position.setCompanyId(dbPosition.getCompanyId());
            position.setCompanyName(dbPosition.getCompanyName());
        } else if (position.getCompanyId() != null) {
            Company company = companyMapper.selectById(position.getCompanyId());
            if (company != null) {
                position.setCompanyName(company.getName());
            }
        }

        Result<?> validateResult = validatePosition(position, true);
        if (validateResult != null) {
            return validateResult;
        }

        positionMapper.updateById(position);

        String companyName = StringUtils.hasText(position.getCompanyName()) ? position.getCompanyName() : dbPosition.getCompanyName();
        String title = StringUtils.hasText(position.getTitle()) ? position.getTitle() : dbPosition.getTitle();
        auditLogService.record(
                "POSITION_UPDATE",
                "POSITION",
                dbPosition.getId(),
                "修改岗位，标题=" + title + "，企业=" + companyName
        );
        return Result.success(null);
    }

    // 删除
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!AuthUtil.hasRole("ADMIN", "TEACHER", "COMPANY")) {
            return Result.forbidden("仅企业/教师/管理员可删除岗位");
        }

        Position position = positionMapper.selectById(id);
        if (position == null) {
            return Result.error("岗位不存在");
        }
        if (AuthUtil.hasRole("COMPANY") && !AuthUtil.hasRole("ADMIN")) {
            User currentUser = AuthUtil.currentUser();
            if (currentUser == null || currentUser.getCompanyId() == null) {
                return Result.error("企业账号未绑定企业信息，请联系管理员");
            }
            if (!currentUser.getCompanyId().equals(position.getCompanyId())) {
                return Result.forbidden("仅可删除本企业岗位");
            }
        }

        positionMapper.deleteById(id);

        auditLogService.record(
                "POSITION_DELETE",
                "POSITION",
                id,
                "删除岗位，标题=" + position.getTitle() + "，企业=" + position.getCompanyName()
        );
        return Result.success(null);
    }

    // 审核岗位
    @PutMapping("/audit")
    public Result<?> audit(@RequestBody Position position) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可审核岗位");
        }
        if (position.getId() == null || position.getAuditStatus() == null) {
            return Result.error("参数不完整");
        }
        
        Position dbPosition = positionMapper.selectById(position.getId());
        if (dbPosition == null) {
            return Result.error("岗位不存在");
        }
        
        Position update = new Position();
        update.setId(position.getId());
        update.setAuditStatus(position.getAuditStatus());
        update.setAuditRemark(position.getAuditRemark());
        positionMapper.updateById(update);
        
        auditLogService.record(
                "POSITION_AUDIT",
                "POSITION",
                position.getId(),
                "审核岗位，结果=" + (position.getAuditStatus() == 1 ? "通过" : "驳回")
        );
        return Result.success(null);
    }

    private Result<?> validatePosition(Position position, boolean requireId) {
        if (requireId && position.getId() == null) {
            return Result.error("岗位ID不能为空");
        }
        if (!StringUtils.hasText(position.getCompanyName())) {
            return Result.error("企业名称不能为空");
        }
        if (!StringUtils.hasText(position.getTitle())) {
            return Result.error("岗位名称不能为空");
        }
        if (!StringUtils.hasText(position.getLocation())) {
            return Result.error("工作地点不能为空");
        }
        if (position.getStatus() != null && position.getStatus() != 0 && position.getStatus() != 1) {
            return Result.error("岗位状态仅支持0或1");
        }
        return null;
    }
}
