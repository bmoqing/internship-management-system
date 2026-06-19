package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Company;
import fun.bmoqing.internship.mapper.CompanyMapper;
import fun.bmoqing.internship.service.AuditLogService;
import fun.bmoqing.internship.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/options")
    public Result<?> options() {
        if (!AuthUtil.hasRole("ADMIN", "TEACHER", "COMPANY")) {
            return Result.forbidden("无权限查看企业选项");
        }

        QueryWrapper<Company> query = new QueryWrapper<>();
        query.eq("status", 1);
        query.orderByAsc("name");
        return Result.success(companyMapper.selectList(query));
    }

    @GetMapping("/my")
    public Result<?> getMyCompany() {
        if (!AuthUtil.hasRole("COMPANY")) {
            return Result.forbidden("仅企业角色可访问");
        }
        User currentUser = AuthUtil.currentUser();
        if (currentUser.getCompanyId() == null) {
            return Result.validationError("当前账号未绑定企业");
        }
        Company company = companyMapper.selectById(currentUser.getCompanyId());
        return Result.success(company);
    }

    @PutMapping("/my")
    public Result<?> updateMyCompany(@RequestBody Company company) {
        if (!AuthUtil.hasRole("COMPANY")) {
            return Result.forbidden("仅企业角色可操作");
        }
        User currentUser = AuthUtil.currentUser();
        if (currentUser.getCompanyId() == null) {
            return Result.validationError("当前账号未绑定企业");
        }
        
        Company dbCompany = companyMapper.selectById(currentUser.getCompanyId());
        if (dbCompany == null) {
            return Result.notFound("企业不存在");
        }

        // Only allow updating specific fields for mentors
        dbCompany.setLatitude(company.getLatitude());
        dbCompany.setLongitude(company.getLongitude());
        dbCompany.setRadius(company.getRadius() != null ? company.getRadius() : 500);
        dbCompany.setWorkStartTime(company.getWorkStartTime());
        dbCompany.setCheckinStartTime(company.getCheckinStartTime());
        dbCompany.setCheckinEndTime(company.getCheckinEndTime());
        dbCompany.setAddress(company.getAddress());
        
        companyMapper.updateById(dbCompany);

        auditLogService.record(
                "COMPANY_CONFIG_UPDATE",
                "COMPANY",
                dbCompany.getId(),
                "企业导师更新考勤配置"
        );
        return Result.success(dbCompany);
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可访问企业管理");
        }

        Page<Company> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Company> query = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like("name", keyword)
                    .or()
                    .like("contact", keyword)
                    .or()
                    .like("phone", keyword));
        }
        query.orderByDesc("id");
        return Result.success(companyMapper.selectPage(page, query));
    }

    @PostMapping
    public Result<?> save(@RequestBody Company company) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可新增企业");
        }

        if (!StringUtils.hasText(company.getName())) {
            return Result.error("企业名称不能为空");
        }

        if (company.getStatus() == null) {
            company.setStatus(1);
        }
        company.setCreateTime(LocalDateTime.now());
        companyMapper.insert(company);

        auditLogService.record(
                "COMPANY_CREATE",
                "COMPANY",
                company.getId(),
                "新增企业，名称=" + company.getName()
        );
        return Result.success(null);
    }

    @PutMapping
    public Result<?> update(@RequestBody Company company) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可修改企业");
        }

        if (company.getId() == null) {
            return Result.error("企业ID不能为空");
        }

        Company dbCompany = companyMapper.selectById(company.getId());
        if (dbCompany == null) {
            return Result.notFound("企业不存在");
        }

        companyMapper.updateById(company);

        auditLogService.record(
                "COMPANY_UPDATE",
                "COMPANY",
                company.getId(),
                "修改企业信息，原名称=" + dbCompany.getName() +
                        (StringUtils.hasText(company.getName()) ? "，新名称=" + company.getName() : "")
        );
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可删除企业");
        }

        Company company = companyMapper.selectById(id);
        if (company == null) {
            return Result.notFound("企业不存在");
        }

        companyMapper.deleteById(id);

        auditLogService.record(
                "COMPANY_DELETE",
                "COMPANY",
                id,
                "删除企业，名称=" + company.getName()
        );
        return Result.success(null);
    }
}
