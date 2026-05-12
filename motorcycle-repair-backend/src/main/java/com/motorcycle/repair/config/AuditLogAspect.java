package com.motorcycle.repair.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motorcycle.repair.annotation.AuditLog;
import com.motorcycle.repair.entity.AuditLogRecord;
import com.motorcycle.repair.filter.JwtAuthenticationFilter;
import com.motorcycle.repair.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;
        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            AuditLogRecord log = new AuditLogRecord();
            log.setModule(auditLog.module());
            log.setOperation(auditLog.operation());
            log.setMethod(point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName());
            try {
                Object[] args = point.getArgs();
                if (args != null && args.length > 0) {
                    String argsStr = objectMapper.writeValueAsString(args);
                    log.setArgs(argsStr.length() > 2000 ? argsStr.substring(0, 2000) : argsStr);
                }
            } catch (Exception ignored) {}
            log.setResult(exception == null ? "SUCCESS" : "FAILED:" + exception.getMessage());
            log.setDurationMs(duration);
            log.setCreateTime(LocalDateTime.now());
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof JwtAuthenticationFilter.UserPrincipal principal) {
                    log.setOperatorId(principal.getUserId());
                    log.setOperatorName(principal.getUsername());
                }
            } catch (Exception ignored) {}
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest req = attrs.getRequest();
                    String ip = req.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) ip = req.getHeader("X-Real-IP");
                    if (ip == null || ip.isEmpty()) ip = req.getRemoteAddr();
                    log.setOperatorIp(ip);
                }
            } catch (Exception ignored) {}
            try { auditLogService.save(log); } catch (Exception ignored) {}
        }
    }
}
