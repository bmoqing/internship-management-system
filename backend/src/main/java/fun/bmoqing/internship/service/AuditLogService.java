package fun.bmoqing.internship.service;

import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.entity.AuditLog;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private static final int MAX_DETAIL_LENGTH = 1000;

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(String action, String targetType, Long targetId, String detail) {
        User operator = AuthUtil.currentUser();
        if (operator == null || operator.getId() == null) {
            return;
        }

        HttpServletRequest request = currentRequest();

        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorId(operator.getId());
        auditLog.setOperatorRole(operator.getRole());
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetail(truncate(detail));
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setRequestMethod(request == null ? null : request.getMethod());
        auditLog.setRequestPath(request == null ? null : request.getRequestURI());
        auditLog.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        if (detail.length() <= MAX_DETAIL_LENGTH) {
            return detail;
        }
        return detail.substring(0, MAX_DETAIL_LENGTH);
    }
}
