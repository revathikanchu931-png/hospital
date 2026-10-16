
package com.mediconnect.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.mediconnect.dto.AuthDtos.DoctorRegistration;
import com.mediconnect.dto.AuthDtos.LaboratoryRegistration;
import com.mediconnect.dto.AuthDtos.PharmacyRegistration;
import com.mediconnect.dto.AuthDtos.RegisterRequest;
import com.mediconnect.dto.AuthDtos.SetupAdminRequest;
import com.mediconnect.entity.User;
import com.mediconnect.enums.AccountStatus;
import com.mediconnect.enums.TokenPurpose;
import com.mediconnect.enums.UserRole;
import com.mediconnect.enums.VerificationStatus;
import com.mediconnect.repository.AccountActionTokenRepository;
import com.mediconnect.repository.AccountAuditRepository;
import com.mediconnect.repository.UserRepository;

class UserRegistrationServiceTest {

    private UserRepository users;
    private PasswordEncoder encoder;
    private AccountActionTokenRepository tokens;
    private UserService service;
    private JdbcTemplate jdbc;
    private AuthenticationManager authenticationManager;
    private AccountAuditRepository auditRepository;
    private EmailDeliveryService emailDeliveryService;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        tokens = mock(AccountActionTokenRepository.class);
        jdbc = mock(JdbcTemplate.class);
        authenticationManager = mock(AuthenticationManager.class);
        auditRepository = mock(AccountAuditRepository.class);

        JavaMailSender mailSender = mock(JavaMailSender.class);

        emailDeliveryService = new EmailDeliveryService(
                mailSender,
                "",
                "",
                "http://localhost:5173"
        );

        service = new UserService(
                users,
                encoder,
                authenticationManager,
                auditRepository,
                tokens,
                emailDeliveryService,
                jdbc,
                "test-initial-admin-setup-code"
        );
    }

    @Test
    void patientUserIsFlushedBeforeProfileForeignKeyInsert() {
        when(users.findByEmailIgnoreCase("patient@example.test"))
                .thenReturn(Optional.empty());

        when(encoder.encode("secure-password-123"))
                .thenReturn("bcrypt-hash");

        when(users.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            user, "id", UUID.randomUUID()
                    );
                    return user;
                });

        var result = service.registerPatient(
                new RegisterRequest(
                        "Test Patient",
                        "patient@example.test",
                        "9000000000",
                        "secure-password-123",
                        "secure-password-123",
                        LocalDate.of(1990, 1, 1),
                        "",
                        "Synthetic address"
                )
        );

        assertEquals("PATIENT", result.role());
        assertEquals(
                AccountStatus.PENDING_EMAIL_VERIFICATION.name(),
                result.accountStatus()
        );
        assertEquals(Boolean.FALSE, result.verificationEmailSent());

        var order = inOrder(users, jdbc);
        order.verify(users).saveAndFlush(any(User.class));
        order.verify(jdbc).update(
                any(String.class), any(), any(), any(), any()
        );
    }

    @Test
    void failedRegistrationEmailDoesNotRollBackAccountOrKeepUnsentToken() {
        when(users.findByEmailIgnoreCase("patient@example.test"))
                .thenReturn(Optional.empty());

        when(encoder.encode("secure-password-123"))
                .thenReturn("bcrypt-hash");

        when(users.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            user, "id", UUID.randomUUID()
                    );
                    return user;
                });

        var result = service.registerPatient(
                new RegisterRequest(
                        "Test Patient",
                        "patient@example.test",
                        "9000000000",
                        "secure-password-123",
                        "secure-password-123",
                        LocalDate.of(1990, 1, 1),
                        "",
                        "Synthetic address"
                )
        );

        assertEquals(Boolean.FALSE, result.verificationEmailSent());

        // The token is deleted before issuing a new one and again if email fails.
        verify(tokens, times(2)).deleteByUser_IdAndPurpose(
                any(UUID.class),
                eq(TokenPurpose.EMAIL_VERIFICATION)
        );
    }

    @Test
    void duplicatePatientEmailIsRejectedBeforeAnyDatabaseWrite() {
        when(users.findByEmailIgnoreCase("patient@example.test"))
                .thenReturn(Optional.of(
                        new User(
                                "Existing Patient",
                                "patient@example.test",
                                "hash",
                                "9000000000",
                                UserRole.PATIENT,
                                VerificationStatus.NOT_REQUIRED
                        )
                ));

        var request = new RegisterRequest(
                "Test Patient",
                "patient@example.test",
                "9000000001",
                "secure-password-123",
                "secure-password-123",
                LocalDate.of(1990, 1, 1),
                "",
                "Synthetic address"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.registerPatient(request)
        );

        assertEquals(IllegalStateException.class, exception.getClass());

        verify(users, never()).saveAndFlush(any(User.class));
        verify(jdbc, never()).update(
                any(String.class), any(), any(), any(), any()
        );
    }

    @Test
    void doctorSelfRegistrationStaysPendingProfessionalApproval() {
        when(users.findByEmailIgnoreCase("doctor@example.test"))
                .thenReturn(Optional.empty());

        when(encoder.encode("secure-password-123"))
                .thenReturn("bcrypt-hash");

        when(users.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            user, "id", UUID.randomUUID()
                    );
                    return user;
                });

        var request = new DoctorRegistration(
                "Test Doctor",
                "doctor@example.test",
                "9000000002",
                "secure-password-123",
                "secure-password-123",
                "MED-123",
                "MBBS",
                "Cardiology",
                5,
                "Demo Clinic",
                "Hyderabad",
                java.math.BigDecimal.valueOf(500)
        );

        var result = service.registerDoctor(request);

        assertEquals("DOCTOR", result.role());
        assertEquals(
                AccountStatus.PENDING_EMAIL_VERIFICATION.name(),
                result.accountStatus()
        );
        assertEquals(Boolean.FALSE, result.verificationEmailSent());
    }

    @Test
    void pharmacySelfRegistrationPersistsAsEmailVerificationPending() {
        when(users.findByEmailIgnoreCase("pharmacy@example.test"))
                .thenReturn(Optional.empty());

        when(encoder.encode("secure-password-123"))
                .thenReturn("bcrypt-hash");

        when(users.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            user, "id", UUID.randomUUID()
                    );
                    return user;
                });

        var request = new PharmacyRegistration(
                "Test Pharmacy",
                "Owner",
                "pharmacy@example.test",
                "9000000003",
                "secure-password-123",
                "secure-password-123",
                "PH-123",
                "Synthetic address"
        );

        var result = service.registerPharmacy(request);

        assertEquals("PHARMACY", result.role());
        assertEquals(
                AccountStatus.PENDING_EMAIL_VERIFICATION.name(),
                result.accountStatus()
        );
        assertEquals(Boolean.FALSE, result.verificationEmailSent());
    }

    @Test
    void laboratorySelfRegistrationPersistsAsEmailVerificationPending() {
        when(users.findByEmailIgnoreCase("lab@example.test"))
                .thenReturn(Optional.empty());

        when(encoder.encode("secure-password-123"))
                .thenReturn("bcrypt-hash");

        when(users.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            user, "id", UUID.randomUUID()
                    );
                    return user;
                });

        var request = new LaboratoryRegistration(
                "Test Lab",
                "Owner",
                "lab@example.test",
                "9000000004",
                "secure-password-123",
                "secure-password-123",
                "LAB-123",
                "Synthetic address"
        );

        var result = service.registerLaboratory(request);

        assertEquals("LABORATORY", result.role());
        assertEquals(
                AccountStatus.PENDING_EMAIL_VERIFICATION.name(),
                result.accountStatus()
        );
        assertEquals(Boolean.FALSE, result.verificationEmailSent());
    }

    @Test
    void adminRegistrationRejectsMissingSetupCodeBeforeCreatingAccount() {
        var request = new SetupAdminRequest(
                "Test Admin",
                "admin@example.test",
                "9000000001",
                "secure-password-123",
                "secure-password-123",
                "Synthetic Organization"
        );

        assertThrows(
                AccessDeniedException.class,
                () -> service.registerInitialAdmin(request, null)
        );

        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void adminRegistrationAcceptsAuthorizedInitialSetupOnlyWhenNoAdminExists() {
        when(users.countByRole(UserRole.ADMIN)).thenReturn(0L);

        when(users.findByEmailIgnoreCase("admin@example.test"))
                .thenReturn(Optional.empty());

        when(encoder.encode("secure-password-123"))
                .thenReturn("bcrypt-hash");

        when(users.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            user, "id", UUID.randomUUID()
                    );
                    return user;
                });

        var request = new SetupAdminRequest(
                "Test Admin",
                "admin@example.test",
                "9000000001",
                "secure-password-123",
                "secure-password-123",
                "Synthetic Organization"
        );

        var result = service.registerInitialAdmin(
                request,
                "test-initial-admin-setup-code"
        );

        assertEquals(UserRole.ADMIN.name(), result.role());
        assertEquals(
                AccountStatus.PENDING_EMAIL_VERIFICATION.name(),
                result.accountStatus()
        );

        verify(users).saveAndFlush(any(User.class));
    }

    public UserRepository getUsers() {
        return users;
    }

    public PasswordEncoder getEncoder() {
        return encoder;
    }
}
