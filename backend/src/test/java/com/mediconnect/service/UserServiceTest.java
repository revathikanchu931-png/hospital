package com.mediconnect.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.mediconnect.dto.AuthDtos.AccountEmailRequest;
import com.mediconnect.dto.AuthDtos.LoginRequest;
import com.mediconnect.dto.AuthDtos.PasswordResetConfirm;
import com.mediconnect.dto.AuthDtos.PasswordResetRequest;
import com.mediconnect.dto.AuthDtos.ProviderCreateRequest;
import com.mediconnect.entity.AccountActionToken;
import com.mediconnect.entity.User;
import com.mediconnect.enums.AccountStatus;
import com.mediconnect.enums.TokenPurpose;
import com.mediconnect.enums.UserRole;
import com.mediconnect.enums.VerificationStatus;
import com.mediconnect.repository.AccountActionTokenRepository;
import com.mediconnect.repository.AccountAuditRepository;
import com.mediconnect.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

class UserServiceTest {
    private UserRepository users;
    private PasswordEncoder encoder;
    private AuthenticationManager manager;
    private AccountAuditRepository audit;
    private AccountActionTokenRepository tokens;
    private EmailDeliveryService email;
    private UserService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        manager = mock(AuthenticationManager.class);
        audit = mock(AccountAuditRepository.class);
        tokens = mock(AccountActionTokenRepository.class);
        email = mock(EmailDeliveryService.class);
        service = new UserService(users, encoder, manager, audit, tokens,
                email, mock(JdbcTemplate.class), "one-time-test-code");
    }

    @Test
    void loginRejectsWrongRoleBeforeCreatingSession() {
        when(manager.authenticate(any(Authentication.class)))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated("p@example.test", "n/a", java.util.List.of()));
        when(users.findByEmailIgnoreCase("p@example.test")).thenReturn(Optional.of(
                new User("Patient", "p@example.test", "hash", "1", UserRole.PATIENT, VerificationStatus.NOT_REQUIRED)));

        assertThrows(BadCredentialsException.class, () -> service.login(
                new LoginRequest("p@example.test", "secret", UserRole.ADMIN), mock(HttpServletRequest.class)));
        verify(users, never()).save(any());
    }

    @Test
    void verifiedProviderPendingApprovalGetsSpecificGateAfterPasswordAuthentication() {
        User doctor = new User("Doctor", "doctor@example.test", "hash", "1", UserRole.DOCTOR,
                VerificationStatus.PENDING, AccountStatus.PENDING_APPROVAL, null);
        when(users.findByEmailIgnoreCase("doctor@example.test")).thenReturn(Optional.of(doctor));
        when(manager.authenticate(any(Authentication.class))).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated("doctor@example.test", "n/a", java.util.List.of()));

        assertThrows(LockedException.class, () -> service.login(new LoginRequest("doctor@example.test", "correct-password", UserRole.DOCTOR), mock(HttpServletRequest.class)));
        verify(manager).authenticate(any(Authentication.class));
    }

    @Test
    void wrongPasswordForPendingProviderDoesNotRevealApprovalState() {
        User doctor = new User("Doctor", "doctor@example.test", "hash", "1", UserRole.DOCTOR,
                VerificationStatus.PENDING, AccountStatus.PENDING_APPROVAL, null);
        when(users.findByEmailIgnoreCase("doctor@example.test")).thenReturn(Optional.of(doctor));
        when(manager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> service.login(new LoginRequest("doctor@example.test", "wrong", UserRole.DOCTOR), mock(HttpServletRequest.class)));
    }

    @Test
    void adminCanProvisionDoctorPendingApproval() {
        when(users.findByEmailIgnoreCase("d@example.test")).thenReturn(Optional.empty());
        when(encoder.encode("a-long-password-1")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });
        User admin = new User("Admin", "a@example.test", "hash", "1", UserRole.ADMIN, VerificationStatus.NOT_REQUIRED);

        var result = service.createProvider(admin,
                new ProviderCreateRequest("Doctor", "d@example.test", "2", "a-long-password-1", UserRole.DOCTOR));

        assertEquals("DOCTOR", result.role());
        assertEquals("PENDING", result.verificationStatus());
        verify(audit).save(any());
    }

    @Test
    void publicProvisionCannotAssignAdmin() {
        User admin = new User("Admin", "a@example.test", "hash", "1", UserRole.ADMIN, VerificationStatus.NOT_REQUIRED);

        assertThrows(IllegalArgumentException.class, () -> service.createProvider(admin,
                new ProviderCreateRequest("Other", "other@example.test", "2", "a-long-password-1", UserRole.ADMIN)));
        verify(users, never()).save(any());
    }

    @Test
    void forgotPasswordForUnknownAccountIsNeutralAndDoesNotNeedMailConfiguration() {
        when(users.findLockedByEmail("missing@example.test")).thenReturn(Optional.empty());

        service.requestPasswordReset(new PasswordResetRequest("missing@example.test", UserRole.PATIENT));

        verify(email, never()).sendPasswordReset(any(), any(), any());
        verify(tokens, never()).save(any());
    }

    @Test
    void passwordResetRequestCreatesExpiringTokenForEligibleVerifiedAccount() {
        User patient = new User("Patient", "patient@example.test", "old-hash", "1",
                UserRole.PATIENT, VerificationStatus.NOT_REQUIRED);
        when(users.findLockedByEmail("patient@example.test")).thenReturn(Optional.of(patient));
        when(email.sendPasswordReset(any(), any(), any())).thenReturn(true);

        service.requestPasswordReset(new PasswordResetRequest("patient@example.test", UserRole.PATIENT));
        verify(tokens).saveAndFlush(any(com.mediconnect.entity.AccountActionToken.class));
        verify(email).sendPasswordReset(eq("patient@example.test"), eq("Patient"), any());
        org.junit.jupiter.api.Assertions.assertNotNull(patient.getPasswordResetEmailSentAt());
    }

    @Test
    void passwordResetCooldownPreventsRepeatedMailRequests() {
        User patient = new User("Patient", "patient@example.test", "old-hash", "1",
                UserRole.PATIENT, VerificationStatus.NOT_REQUIRED);
        patient.setPasswordResetEmailSentAt(Instant.now());
        when(users.findLockedByEmail("patient@example.test")).thenReturn(Optional.of(patient));

        service.requestPasswordReset(new PasswordResetRequest("patient@example.test", UserRole.PATIENT));

        verify(email, never()).sendPasswordReset(any(), any(), any());
        verify(tokens, never()).save(any());
    }

    @Test
    void resendVerificationKeepsPendingApprovalAccountUnchangedAndHonorsCooldown() {
        User doctor = new User("Doctor", "doctor@example.test", "hash", "1", UserRole.DOCTOR,
                VerificationStatus.PENDING, AccountStatus.PENDING_EMAIL_VERIFICATION, null);
        ReflectionTestUtils.setField(doctor, "id", UUID.randomUUID());
        doctor.setVerificationEmailSentAt(Instant.now());
        when(users.findLockedByEmail("doctor@example.test")).thenReturn(Optional.of(doctor));

        service.resendVerification(new AccountEmailRequest("doctor@example.test", UserRole.DOCTOR));

        verify(email, never()).sendVerification(any(), any(), any());
        assertEquals(AccountStatus.PENDING_EMAIL_VERIFICATION, doctor.getAccountStatus());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void emailVerificationAppliesExpectedActivationPolicyAndConsumesToken(UserRole role) {
        String address = role.name().toLowerCase() + "@example.test";
        VerificationStatus verification = java.util.Set.of(UserRole.DOCTOR, UserRole.PHARMACY, UserRole.LABORATORY).contains(role)
                ? VerificationStatus.PENDING : VerificationStatus.NOT_REQUIRED;
        User account = new User(role.name(), address, "hash", "1", role,
                verification, AccountStatus.PENDING_EMAIL_VERIFICATION, null);
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        AccountActionToken token = new AccountActionToken(account, "hash", TokenPurpose.EMAIL_VERIFICATION,
                Instant.now().plusSeconds(600));
        when(tokens.findFirstByTokenHashAndPurposeAndUsedAtIsNullAndExpiresAtAfter(anyString(),
                eq(TokenPurpose.EMAIL_VERIFICATION), any(Instant.class))).thenReturn(Optional.of(token));

        var response = service.verifyEmail("one-time-verification-token");

        AccountStatus expected = java.util.Set.of(UserRole.DOCTOR, UserRole.PHARMACY, UserRole.LABORATORY).contains(role)
                ? AccountStatus.PENDING_APPROVAL : AccountStatus.ACTIVE;
        assertEquals(expected.name(), response.accountStatus());
        assertEquals(role.name(), response.role());
        assertEquals(expected, account.getAccountStatus());
        org.junit.jupiter.api.Assertions.assertTrue(account.isEmailVerified());
        org.junit.jupiter.api.Assertions.assertFalse(token.isUsable(Instant.now()));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void passwordResetRequestTargetsEachEligibleRole(UserRole role) {
        String address = role.name().toLowerCase() + "@example.test";
        User account = new User(role.name(), address, "old-hash", "1", role,
                VerificationStatus.NOT_REQUIRED, AccountStatus.ACTIVE, null);
        when(users.findLockedByEmail(address)).thenReturn(Optional.of(account));
        when(email.sendPasswordReset(any(), any(), any())).thenReturn(true);

        service.requestPasswordReset(new PasswordResetRequest(address, role));

        verify(email).sendPasswordReset(eq(address), eq(role.name()), any());
        verify(tokens).saveAndFlush(any(AccountActionToken.class));
    }

    @Test
    void passwordResetDoesNotChangeDoctorApprovalOrVerificationStatus() {
        User doctor = new User("Doctor", "doctor@example.test", "old-hash", "1", UserRole.DOCTOR,
                VerificationStatus.PENDING, AccountStatus.PENDING_APPROVAL, null);
        AccountActionToken token = new AccountActionToken(doctor, "hash", TokenPurpose.PASSWORD_RESET,
                Instant.now().plusSeconds(600));
        when(tokens.findFirstByTokenHashAndPurposeAndUsedAtIsNullAndExpiresAtAfter(anyString(),
                eq(TokenPurpose.PASSWORD_RESET), any(Instant.class))).thenReturn(Optional.of(token));
        when(encoder.encode("new-secure-password-123")).thenReturn("new-bcrypt-hash");

        service.resetPassword(new PasswordResetConfirm("one-time-reset-token", "new-secure-password-123",
                "new-secure-password-123"));

        assertEquals("new-bcrypt-hash", doctor.getPasswordHash());
        assertEquals(AccountStatus.PENDING_APPROVAL, doctor.getAccountStatus());
        assertEquals(VerificationStatus.PENDING, doctor.getVerificationStatus());
    }
}
