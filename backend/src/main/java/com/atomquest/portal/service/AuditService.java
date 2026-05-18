package com.atomquest.portal.service;

import com.atomquest.portal.entity.AuditLog;
import com.atomquest.portal.entity.User;
import com.atomquest.portal.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void log(User user, String action, String entityType, Long entityId, String oldValue, String newValue) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    public Page<AuditLog> list(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
