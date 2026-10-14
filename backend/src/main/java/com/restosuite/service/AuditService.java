package com.restosuite.service;

import com.restosuite.domain.AuditLog;
import com.restosuite.repo.AuditLogRepository;
import com.restosuite.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditLogRepository logs;
    private final Support support;

    public AuditService(AuditLogRepository logs, Support support) {
        this.logs = logs;
        this.support = support;
    }

    public void record(String action, String entityType, Object entityId, String details) {
        AuthUser user = support.user();
        AuditLog log = new AuditLog();
        log.setRestaurantId(user.getRestaurantId());
        log.setUserId(user.getId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId == null ? null : entityId.toString());
        log.setDetails(details);
        log.setIpAddress(ip());
        logs.save(log);
    }

    private String ip() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            HttpServletRequest request = servlet.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
