
package com.mediconnect.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediconnect.dto.AuthDtos.AccountEmailRequest;
import com.mediconnect.dto.AuthDtos.DoctorRegistration;
import com.mediconnect.dto.AuthDtos.LaboratoryRegistration;
import com.mediconnect.dto.AuthDtos.LoginRequest;
import com.mediconnect.dto.AuthDtos.PasswordResetConfirm;
import com.mediconnect.dto.AuthDtos.PasswordResetRequest;
import com.mediconnect.dto.AuthDtos.PharmacyRegistration;
import com.mediconnect.dto.AuthDtos.ProfileUpdate;
import com.mediconnect.dto.AuthDtos.ProviderCreateRequest;
import com.mediconnect.dto.AuthDtos.RegisterRequest;
import com.mediconnect.dto.AuthDtos.SetupAdminRequest;
import com.mediconnect.dto.AuthDtos.UserResponse;
import com.mediconnect.entity.AccountActionToken;
import com.mediconnect.entity.AccountAuditEvent;
import com.mediconnect.entity.User;
import com.mediconnect.enums.AccountStatus;
import com.mediconnect.enums.TokenPurpose;
import com.mediconnect.enums.UserRole;
import com.mediconnect.enums.VerificationStatus;
import com.mediconnect.repository.AccountActionTokenRepository;
import com.mediconnect.repository.AccountAuditRepository;
import com.mediconnect.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Service
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration EMAIL_COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final AccountAuditRepository audit;
    private final AccountActionTokenRepository tokens;
    private final EmailDeliveryService email;
    private final JdbcTemplate jdbc;
    private final String setupToken;
    @Value("${app.email-verification-required:false}")
    private boolean emailVerificationRequired;

    public UserService(
            UserRepository users,
            PasswordEncoder encoder,
            AuthenticationManager authManager,
            AccountAuditRepository audit,
            AccountActionTokenRepository tokens,
            EmailDeliveryService email,
            JdbcTemplate jdbc,
            @Value("${app.admin-setup-token:}") String setupToken) {

        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.audit = audit;
        this.tokens = tokens;
        this.email = email;
        this.jdbc = jdbc;
        this.setupToken = setupToken;
    }

    public UserResponse publicUser(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getVerificationStatus().name(),
                user.getAccountStatus().name(),
                user.isEmailVerified()
        );
    }

    @Transactional
    public UserResponse registerPatient(RegisterRequest request) {
        requireMatchingPasswords(request.password(), request.confirmPassword());

        if (request.dateOfBirth().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Date of birth cannot be in the future."
            );
        }

        User user = pendingUser(
                request.name(),
                request.email(),
                request.phone(),
                request.password(),
                UserRole.PATIENT,
                VerificationStatus.NOT_REQUIRED,
                null
        );

        jdbc.update(
                "INSERT INTO patient_profiles " +
                "(user_id,birth_date,gender,address) " +
                "VALUES (UUID_TO_BIN(?),?,?,?)",
                user.getId().toString(),
                request.dateOfBirth(),
                request.gender(),
                request.address()
        );

        return registrationResponse(user, sendVerificationIfRequired(user));
    }

    @Transactional
    public UserResponse registerDoctor(DoctorRegistration request) {
        requireMatchingPasswords(
                request.password(),
                request.confirmPassword()
        );

        User user = pendingUser(
                request.name(),
                request.email(),
                request.phone(),
                request.password(),
                UserRole.DOCTOR,
                VerificationStatus.PENDING,
                null
        );

        jdbc.update(
                "INSERT INTO doctor_profiles " +
                "(user_id,specialization,qualification,experience_years," +
                "consultation_fee,clinic_name,city,registration_number) " +
                "VALUES (UUID_TO_BIN(?),?,?,?,?,?,?,?)",
                user.getId().toString(),
                request.specialization().trim(),
                request.qualification().trim(),
                request.experienceYears(),
                request.consultationFee(),
                request.clinicName().trim(),
                request.city().trim(),
                request.registrationNumber().trim()
        );

        return registrationResponse(user, sendVerificationIfRequired(user));
    }

    @Transactional
    public UserResponse registerPharmacy(PharmacyRegistration request) {
        requireMatchingPasswords(
                request.password(),
                request.confirmPassword()
        );

        User user = pendingUser(
                request.pharmacyName(),
                request.email(),
                request.phone(),
                request.password(),
                UserRole.PHARMACY,
                VerificationStatus.PENDING,
                null
        );

        jdbc.update(
                "INSERT INTO pharmacy_profiles " +
                "(user_id,contact_person,address,license_details) " +
                "VALUES (UUID_TO_BIN(?),?,?,?)",
                user.getId().toString(),
                request.ownerName().trim(),
                request.address().trim(),
                request.licenseNumber().trim()
        );

        return registrationResponse(user, sendVerificationIfRequired(user));
    }

    @Transactional
    public UserResponse registerLaboratory(
            LaboratoryRegistration request) {

        requireMatchingPasswords(
                request.password(),
                request.confirmPassword()
        );

        User user = pendingUser(
                request.laboratoryName(),
                request.email(),
                request.phone(),
                request.password(),
                UserRole.LABORATORY,
                VerificationStatus.PENDING,
                null
        );

        jdbc.update(
                "INSERT INTO laboratory_profiles " +
                "(user_id,contact_person,address,license_details) " +
                "VALUES (UUID_TO_BIN(?),?,?,?)",
                user.getId().toString(),
                request.ownerName().trim(),
                request.address().trim(),
                request.accreditationDetails().trim()
        );

        return registrationResponse(user, sendVerificationIfRequired(user));
    }

    @Transactional
    public UserResponse registerInitialAdmin(
            SetupAdminRequest request,
            String providedToken) {

        requireMatchingPasswords(
                request.password(),
                request.confirmPassword()
        );

        if (setupToken.isBlank()
                || !constantTimeEquals(setupToken, providedToken)) {
            throw new org.springframework.security.access
                    .AccessDeniedException(
                    "Administrator setup is not authorized."
            );
        }

        if (users.countByRole(UserRole.ADMIN) > 0) {
            throw new IllegalStateException(
                    "Initial administrator setup has already been used."
            );
        }

        User user = pendingUser(
                request.name(),
                request.email(),
                request.phone(),
                request.password(),
                UserRole.ADMIN,
                VerificationStatus.NOT_REQUIRED,
                request.organizationName()
        );

        return registrationResponse(user, sendVerificationIfRequired(user));
    }

    private User pendingUser(
            String name,
            String rawEmail,
            String phone,
            String password,
            UserRole role,
            VerificationStatus verification,
            String organization) {

        String normalizedEmail =
                rawEmail.trim().toLowerCase(Locale.ROOT);

        if (users.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalStateException(
                    "An account with this email already exists."
            );
        }

        boolean verifiedForDevelopment = !emailVerificationRequired;
        boolean providerAccount = role == UserRole.DOCTOR
                || role == UserRole.PHARMACY
                || role == UserRole.LABORATORY;
        AccountStatus initialStatus = verifiedForDevelopment
                ? providerAccount
                    ? AccountStatus.PENDING_APPROVAL
                    : AccountStatus.ACTIVE
                : AccountStatus.PENDING_EMAIL_VERIFICATION;
        User user = new User(
                        name.trim(),
                        normalizedEmail,
                        encoder.encode(password),
                        phone,
                        role,
                        verification,
                        initialStatus,
                        organization
                );
        user.setEmailVerified(verifiedForDevelopment);
        return users.saveAndFlush(user);
    }

    private UserResponse registrationResponse(
            User user,
            boolean verificationEmailSent) {

        return new UserResponse(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getVerificationStatus().name(),
                user.getAccountStatus().name(),
                user.isEmailVerified(),
                verificationEmailSent
        );
    }

    private boolean issueVerification(User user) {
        tokens.deleteByUser_IdAndPurpose(
                user.getId(),
                TokenPurpose.EMAIL_VERIFICATION
        );

        String raw = newToken();
        Instant now = Instant.now();

        tokens.save(
                new AccountActionToken(
                        user,
                        hash(raw),
                        TokenPurpose.EMAIL_VERIFICATION,
                        now.plus(24, ChronoUnit.HOURS)
                )
        );

        user.setVerificationEmailSentAt(now);

        boolean sent = email.sendVerification(
                user.getEmail(),
                user.getName(),
                raw
        );

        if (!sent) {
            tokens.deleteByUser_IdAndPurpose(
                    user.getId(),
                    TokenPurpose.EMAIL_VERIFICATION
            );
        }

        return sent;
    }

    private boolean sendVerificationIfRequired(User user) {
        return emailVerificationRequired && issueVerification(user);
    }

    @Transactional
    public void resendVerification(AccountEmailRequest request) {
        String address =
                request.email().trim().toLowerCase(Locale.ROOT);

        users.findLockedByEmail(address)
                .filter(user ->
                        user.getRole() == request.role()
                        && !user.isEmailVerified()
                        && user.getAccountStatus()
                            != AccountStatus.DISABLED
                        && user.getAccountStatus()
                            != AccountStatus.REJECTED
                )
                .ifPresent(user -> {
                    Instant now = Instant.now();

                    if (!cooldownElapsed(
                            user.getVerificationEmailSentAt(), now)) {
                        return;
                    }

                    if (!issueVerification(user)) {
                        throw new EmailDeliveryUnavailableException();
                    }
                });
    }

    @Transactional
    public UserResponse verifyEmail(String rawToken) {
        AccountActionToken token =
                validToken(rawToken, TokenPurpose.EMAIL_VERIFICATION);

        User user = token.getUser();
        token.markUsed();
        user.setEmailVerified(true);

        if (user.getRole() == UserRole.DOCTOR
                || user.getRole() == UserRole.PHARMACY
                || user.getRole() == UserRole.LABORATORY) {
            user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        } else {
            user.setAccountStatus(AccountStatus.ACTIVE);
        }

        return publicUser(user);
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String address =
                request.email().trim().toLowerCase(Locale.ROOT);

        users.findLockedByEmail(address)
                .filter(user ->
                        user.getRole() == request.role()
                        && user.isEmailVerified()
                        && user.getAccountStatus()
                            != AccountStatus.DISABLED
                        && user.getAccountStatus()
                            != AccountStatus.REJECTED
                )
                .ifPresent(user -> {
                    Instant now = Instant.now();

                    if (!cooldownElapsed(
                            user.getPasswordResetEmailSentAt(), now)) {
                        return;
                    }

                    tokens.deleteByUser_IdAndPurpose(
                            user.getId(),
                            TokenPurpose.PASSWORD_RESET
                    );

                    String raw = newToken();

                    AccountActionToken resetToken =
                            tokens.saveAndFlush(
                                    new AccountActionToken(
                                            user,
                                            hash(raw),
                                            TokenPurpose.PASSWORD_RESET,
                                            now.plus(
                                                    30,
                                                    ChronoUnit.MINUTES
                                            )
                                    )
                            );

                    if (!email.sendPasswordReset(
                            user.getEmail(),
                            user.getName(),
                            raw)) {

                        resetToken.markUsed();
                        throw new EmailDeliveryUnavailableException();
                    }

                    user.setPasswordResetEmailSentAt(now);
                });
    }

    private static boolean cooldownElapsed(
            Instant lastSentAt,
            Instant now) {

        return lastSentAt == null
                || !lastSentAt.plus(EMAIL_COOLDOWN).isAfter(now);
    }

    @Transactional
    public void resetPassword(PasswordResetConfirm request) {
        requireMatchingPasswords(
                request.password(),
                request.confirmPassword()
        );

        AccountActionToken token =
                validToken(request.token(), TokenPurpose.PASSWORD_RESET);

        token.getUser().setPasswordHash(
                encoder.encode(request.password())
        );

        token.markUsed();
    }

    private AccountActionToken validToken(
            String raw,
            TokenPurpose purpose) {

        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "This link is invalid or has expired."
            );
        }

        return tokens.findFirstByTokenHashAndPurposeAndUsedAtIsNullAndExpiresAtAfter(
                        hash(raw),
                        purpose,
                        Instant.now()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "This link is invalid or has expired."
                        )
                );
    }

    private static void requireMatchingPasswords(
            String password,
            String confirmation) {

        if (password == null || !password.equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Passwords do not match."
            );
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Secure token hashing is unavailable.",
                    exception
            );
        }
    }

    private static boolean constantTimeEquals(
            String expected,
            String actual) {

        return actual != null
                && MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        actual.getBytes(StandardCharsets.UTF_8)
                );
    }

    @Transactional
    public UserResponse login(
            LoginRequest request,
            HttpServletRequest servletRequest) {

        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        User user = users.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password."
                        )
                );

        if (user.getRole() != request.role()) {
            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }

        var result = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.password()
                )
        );

        // Development may skip the email link. Promote only after the
        // password has been verified, and retain provider approval gates.
        if (!emailVerificationRequired && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            boolean providerAccount = user.getRole() == UserRole.DOCTOR
                    || user.getRole() == UserRole.PHARMACY
                    || user.getRole() == UserRole.LABORATORY;
            user.setAccountStatus(providerAccount
                    ? AccountStatus.PENDING_APPROVAL
                    : AccountStatus.ACTIVE);
        }

        if (user.getAccountStatus() == AccountStatus.PENDING_APPROVAL
                || user.getVerificationStatus()
                    == VerificationStatus.PENDING) {
            throw new org.springframework.security.authentication
                    .LockedException(
                    "Your account is awaiting administrator approval. "
                    + "Provider features remain unavailable until approval."
            );
        }

        if (servletRequest.getSession(false) != null) {
            servletRequest.changeSessionId();
        }

        SecurityContextHolder.getContext().setAuthentication(result);

        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        return publicUser(user);
    }

    public static class EmailDeliveryUnavailableException
            extends RuntimeException {

        public EmailDeliveryUnavailableException() {
            super(
                    "Email delivery is temporarily unavailable. "
                    + "Please try again later."
            );
        }
    }

    @Transactional
    public UserResponse createProvider(
            User actor,
            ProviderCreateRequest request) {

        UserRole role = request.role();

        if (!java.util.Set.of(
                UserRole.DOCTOR,
                UserRole.PHARMACY,
                UserRole.LABORATORY
        ).contains(role)) {
            throw new IllegalArgumentException(
                    "Only doctor, pharmacy and laboratory accounts "
                    + "can be provisioned here."
            );
        }

        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        if (users.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalStateException(
                    "An account with this email already exists."
            );
        }

        VerificationStatus status =
                role == UserRole.DOCTOR
                        ? VerificationStatus.PENDING
                        : VerificationStatus.APPROVED;

        AccountStatus accountStatus =
                role == UserRole.DOCTOR
                        ? AccountStatus.PENDING_APPROVAL
                        : AccountStatus.ACTIVE;

        User user = users.save(
                new User(
                        request.name().trim(),
                        normalizedEmail,
                        encoder.encode(request.password()),
                        request.phone(),
                        role,
                        status,
                        accountStatus,
                        null
                )
        );

        audit.save(
                new AccountAuditEvent(actor, user, "ACCOUNT_CREATED")
        );

        return publicUser(user);
    }

    @Transactional
    public boolean setActive(
            User actor,
            UUID id,
            boolean active) {

        if (actor.getId().equals(id) && !active) {
            throw new IllegalArgumentException(
                    "You cannot deactivate your own account."
            );
        }

        User target = users.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        if (target.getRole() == UserRole.ADMIN
                && target.getId().equals(actor.getId())) {
            throw new IllegalArgumentException(
                    "You cannot change your own administrator account."
            );
        }

        if (active) {
            if (emailVerificationRequired && !target.isEmailVerified()) {
                throw new IllegalStateException(
                        "Verify the account email before activating it."
                );
            }

            if (!emailVerificationRequired) {
                target.setEmailVerified(true);
            }

            target.setActive(
                    target.getVerificationStatus()
                            == VerificationStatus.NOT_REQUIRED
                    || target.getVerificationStatus()
                            == VerificationStatus.APPROVED
            );

            if (!target.isActive()) {
                throw new IllegalStateException(
                        "Approve the provider before activating the account."
                );
            }
        } else {
            target.setActive(false);
        }

        audit.save(
                new AccountAuditEvent(
                        actor,
                        target,
                        active ? "ACCOUNT_ACTIVATED"
                               : "ACCOUNT_DEACTIVATED"
                )
        );

        return target.isActive();
    }

    @Transactional
    public UserResponse verifyProvider(
            User actor,
            UUID id,
            boolean approved) {

        User target = users.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Provider not found.")
                );

        if (target.getRole() != UserRole.DOCTOR
                && target.getRole() != UserRole.PHARMACY
                && target.getRole() != UserRole.LABORATORY) {
            throw new IllegalArgumentException(
                    "This account is not a provider."
            );
        }

        if (approved && emailVerificationRequired
                && !target.isEmailVerified()) {
            throw new IllegalStateException(
                    "The provider must verify their email before approval."
            );
        }

        if (approved && !emailVerificationRequired) {
            target.setEmailVerified(true);
        }

        boolean wasDisabled =
                target.getAccountStatus() == AccountStatus.DISABLED;

        target.setVerificationStatus(
                approved
                        ? VerificationStatus.APPROVED
                        : VerificationStatus.REJECTED
        );

        if (approved && target.isEmailVerified() && !wasDisabled) {
            target.setAccountStatus(AccountStatus.ACTIVE);
        }

        audit.save(
                new AccountAuditEvent(
                        actor,
                        target,
                        approved ? "PROVIDER_APPROVED"
                                 : "PROVIDER_REJECTED"
                )
        );

        return publicUser(target);
    }

    public User current(
            org.springframework.security.core.Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.security.access
                    .AccessDeniedException("Authentication required");
        }

        User user = users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new org.springframework.security.access
                                .AccessDeniedException(
                                "Authentication required"
                        )
                );

        if (!user.isActive()
                || !user.isEmailVerified()
                || user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new org.springframework.security.access
                    .AccessDeniedException(
                    "Account access is not enabled."
            );
        }

        return user;
    }

    public java.util.Map<String, Object> profile(User user) {
        java.util.Map<String, Object> result =
                new java.util.LinkedHashMap<>();

        result.put("id", user.getId().toString());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("role", user.getRole().name());
        result.put("accountStatus", user.getAccountStatus().name());
        result.put("organizationName", user.getOrganizationName());

        String table = switch (user.getRole()) {
            case PATIENT -> "patient_profiles";
            case DOCTOR -> "doctor_profiles";
            case PHARMACY -> "pharmacy_profiles";
            case LABORATORY -> "laboratory_profiles";
            case ADMIN -> null;
        };

        if (table != null) {
            var rows = jdbc.queryForList(
                    "SELECT * FROM " + table
                    + " WHERE user_id=UUID_TO_BIN(?)",
                    user.getId().toString()
            );

            if (!rows.isEmpty()) {
                var row = new java.util.LinkedHashMap<>(rows.get(0));
                row.remove("user_id");
                result.put("profile", row);
            }
        }

        return result;
    }

    @Transactional
    public java.util.Map<String, Object> updateProfile(
            User user,
            ProfileUpdate request) {

        User managed = users.findById(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found.")
                );

        managed.setName(request.name().trim());
        managed.setPhone(request.phone());

        return profile(managed);
    }

    public void logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        new org.springframework.security.web.authentication.logout
                .SecurityContextLogoutHandler()
                .logout(request, response, null);
    }
}
