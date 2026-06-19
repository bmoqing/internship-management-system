package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Notice;
import fun.bmoqing.internship.mapper.NoticeMapper;
import fun.bmoqing.internship.service.AuditLogService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeMapper noticeMapper;
    private final AuditLogService auditLogService;

    public NoticeController(NoticeMapper noticeMapper, AuditLogService auditLogService) {
        this.noticeMapper = noticeMapper;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可访问公告管理");
        }

        Page<Notice> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Notice> query = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like("title", keyword)
                    .or()
                    .like("content", keyword));
        }
        query.orderByDesc("create_time").orderByDesc("id");
        return Result.success(noticeMapper.selectPage(page, query));
    }

    @PostMapping
    public Result<?> save(@RequestBody Notice notice) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可新增公告");
        }

        if (!StringUtils.hasText(notice.getTitle())) {
            return Result.error("公告标题不能为空");
        }
        if (!StringUtils.hasText(notice.getContent())) {
            return Result.error("公告内容不能为空");
        }

        if (!StringUtils.hasText(notice.getLevel())) {
            notice.setLevel("INFO");
        } else {
            notice.setLevel(notice.getLevel().toUpperCase());
        }
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);

        auditLogService.record(
                "NOTICE_CREATE",
                "NOTICE",
                notice.getId(),
                "新增公告，标题=" + notice.getTitle()
        );
        return Result.success(null);
    }

    @PutMapping
    public Result<?> update(@RequestBody Notice notice) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可修改公告");
        }

        if (notice.getId() == null) {
            return Result.error("公告ID不能为空");
        }

        Notice dbNotice = noticeMapper.selectById(notice.getId());
        if (dbNotice == null) {
            return Result.notFound("公告不存在");
        }

        if (StringUtils.hasText(notice.getLevel())) {
            notice.setLevel(notice.getLevel().toUpperCase());
        }
        noticeMapper.updateById(notice);

        auditLogService.record(
                "NOTICE_UPDATE",
                "NOTICE",
                notice.getId(),
                "修改公告，原标题=" + dbNotice.getTitle() +
                        (StringUtils.hasText(notice.getTitle()) ? "，新标题=" + notice.getTitle() : "")
        );
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可删除公告");
        }

        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            return Result.notFound("公告不存在");
        }

        noticeMapper.deleteById(id);

        auditLogService.record(
                "NOTICE_DELETE",
                "NOTICE",
                id,
                "删除公告，标题=" + notice.getTitle()
        );
        return Result.success(null);
    }

    @GetMapping("/public")
    public Result<?> publicList(@RequestParam(defaultValue = "5") Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<Notice> notices = noticeMapper.selectLatestActive(safeLimit);
        return Result.success(notices);
    }
}
