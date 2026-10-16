package com.mediconnect.entity;
import jakarta.persistence.*;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="account_audit_events",indexes=@Index(name="idx_account_audit_target_time",columnList="target_id,created_at"))
public class AccountAuditEvent{
 @Id @GeneratedValue(strategy=GenerationType.UUID) @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="actor_id",nullable=false) private User actor;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="target_id",nullable=false) private User target;
 @Column(nullable=false,length=40) private String action;
 @Column(nullable=false,name="created_at",updatable=false) private Instant createdAt=Instant.now();
 protected AccountAuditEvent(){}public AccountAuditEvent(User actor,User target,String action){this.actor=actor;this.target=target;this.action=action;}
}
