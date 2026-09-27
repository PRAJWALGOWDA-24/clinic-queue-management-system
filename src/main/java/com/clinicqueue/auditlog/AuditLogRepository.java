// auditlog/AuditLogRepository.java
package com.clinicqueue.auditlog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}