package com.mediconnect.repository;
import com.mediconnect.entity.AccountAuditEvent;import org.springframework.data.jpa.repository.JpaRepository;import java.util.UUID;
public interface AccountAuditRepository extends JpaRepository<AccountAuditEvent,UUID>{}
