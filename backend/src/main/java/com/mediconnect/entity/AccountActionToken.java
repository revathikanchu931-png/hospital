package com.mediconnect.entity;
import com.mediconnect.enums.TokenPurpose;import jakarta.persistence.*;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="account_action_tokens",uniqueConstraints=@UniqueConstraint(name="uk_account_token_hash",columnNames="token_hash"),indexes=@Index(name="idx_account_token_user_purpose",columnList="user_id,purpose,expires_at"))
public class AccountActionToken{
 @Id @GeneratedValue(strategy=GenerationType.UUID) @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
 @JdbcTypeCode(SqlTypes.CHAR) @Column(name="token_hash",nullable=false,length=64,columnDefinition="CHAR(64)") private String tokenHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private TokenPurpose purpose;
 @Column(name="expires_at",nullable=false) private Instant expiresAt;
 @Column(name="used_at") private Instant usedAt;
 @Column(nullable=false,name="created_at",updatable=false) private Instant createdAt=Instant.now();
 protected AccountActionToken(){}
 public AccountActionToken(User user,String tokenHash,TokenPurpose purpose,Instant expiresAt){this.user=user;this.tokenHash=tokenHash;this.purpose=purpose;this.expiresAt=expiresAt;}
 public User getUser(){return user;}public boolean isUsable(Instant now){return usedAt==null&&expiresAt.isAfter(now);}public void markUsed(){usedAt=Instant.now();}
}
