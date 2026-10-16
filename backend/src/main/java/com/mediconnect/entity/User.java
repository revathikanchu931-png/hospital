package com.mediconnect.entity;
import com.mediconnect.enums.UserRole;
import com.mediconnect.enums.VerificationStatus;
import com.mediconnect.enums.AccountStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="users", uniqueConstraints=@UniqueConstraint(name="uk_users_email",columnNames="email"), indexes=@Index(name="idx_user_role_active",columnList="role,active"))
public class User {
 @Id @GeneratedValue(strategy=GenerationType.UUID) @JdbcTypeCode(SqlTypes.BINARY) @Column(columnDefinition="BINARY(16)") private UUID id;
 @Column(nullable=false,length=140) private String name;
 @Column(nullable=false,length=190) private String email;
 @Column(nullable=false,name="password_hash",length=100) private String passwordHash;
 @Column(length=30) private String phone;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private UserRole role;
 @Enumerated(EnumType.STRING) @Column(nullable=false,name="verification_status",length=20) private VerificationStatus verificationStatus;
 @Column(nullable=false) private boolean active=true;
 @Column(name="email_verified",nullable=false) private boolean emailVerified=true;
 @Enumerated(EnumType.STRING) @Column(name="account_status",nullable=false,length=32) private AccountStatus accountStatus=AccountStatus.ACTIVE;
 @Column(name="organization_name",length=200) private String organizationName;
 @Column(name="verification_email_sent_at") private Instant verificationEmailSentAt;
 @Column(name="password_reset_email_sent_at") private Instant passwordResetEmailSentAt;
 @Column(nullable=false,name="created_at",updatable=false) private Instant createdAt=Instant.now();
 protected User() {}
 public User(String name,String email,String passwordHash,String phone,UserRole role,VerificationStatus verificationStatus){this.name=name;this.email=email;this.passwordHash=passwordHash;this.phone=phone;this.role=role;this.verificationStatus=verificationStatus;}
 public User(String name,String email,String passwordHash,String phone,UserRole role,VerificationStatus verificationStatus,AccountStatus accountStatus,String organizationName){this(name,email,passwordHash,phone,role,verificationStatus);this.accountStatus=accountStatus;this.active=accountStatus==AccountStatus.ACTIVE;this.emailVerified=accountStatus!=AccountStatus.PENDING_EMAIL_VERIFICATION;this.organizationName=organizationName;}
 public UUID getId(){return id;} public String getName(){return name;} public void setName(String name){this.name=name;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String passwordHash){this.passwordHash=passwordHash;} public String getPhone(){return phone;} public void setPhone(String phone){this.phone=phone;} public UserRole getRole(){return role;} public VerificationStatus getVerificationStatus(){return verificationStatus;} public boolean isActive(){return active;} public boolean isEmailVerified(){return emailVerified;} public AccountStatus getAccountStatus(){return accountStatus;} public String getOrganizationName(){return organizationName;} public Instant getVerificationEmailSentAt(){return verificationEmailSentAt;} public void setVerificationEmailSentAt(Instant sentAt){this.verificationEmailSentAt=sentAt;} public Instant getPasswordResetEmailSentAt(){return passwordResetEmailSentAt;} public void setPasswordResetEmailSentAt(Instant sentAt){this.passwordResetEmailSentAt=sentAt;} public void setActive(boolean active){this.active=active;this.accountStatus=active?AccountStatus.ACTIVE:AccountStatus.DISABLED;} public void setEmailVerified(boolean value){this.emailVerified=value;} public void setAccountStatus(AccountStatus status){this.accountStatus=status;this.active=status==AccountStatus.ACTIVE;} public void setVerificationStatus(VerificationStatus status){this.verificationStatus=status;if(status==VerificationStatus.APPROVED&&emailVerified&&active)this.accountStatus=AccountStatus.ACTIVE;else if(status==VerificationStatus.REJECTED)this.accountStatus=AccountStatus.REJECTED;else if(status==VerificationStatus.PENDING)this.accountStatus=AccountStatus.PENDING_APPROVAL;} public Instant getCreatedAt(){return createdAt;}
}
