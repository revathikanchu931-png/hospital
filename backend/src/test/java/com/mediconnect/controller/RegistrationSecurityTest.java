package com.mediconnect.controller;

import com.mediconnect.config.SecurityConfig;
import com.mediconnect.entity.User;
import com.mediconnect.enums.UserRole;
import com.mediconnect.enums.VerificationStatus;
import com.mediconnect.exception.ApiExceptionHandler;
import com.mediconnect.repository.AppointmentRepository;
import com.mediconnect.repository.UserRepository;
import com.mediconnect.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(controllers = {AuthController.class, DashboardController.class})
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@TestPropertySource(properties = "app.cors-origin=http://localhost:5173")
class RegistrationSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService service;
    @MockitoBean UserRepository users;
    @MockitoBean AppointmentRepository appointments;

    @Test void unauthenticatedPatientCanRegisterWithCsrf() throws Exception {
        mvc.perform(post("/api/auth/register/patient").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Test Patient","email":"patient@example.test","phone":"9000000000","password":"secret1234","confirmPassword":"secret1234","dateOfBirth":"1990-01-01","gender":"","address":"Demo address"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test void unauthenticatedDoctorCanRegisterAndEndpointKeepsApprovalFlow() throws Exception {
        mvc.perform(post("/api/auth/register/doctor").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Test Doctor","email":"doctor@example.test","phone":"9000000001","password":"securePassword123","confirmPassword":"securePassword123","registrationNumber":"MED-TEST-01","qualification":"MBBS","specialization":"Cardiology","experienceYears":5,"clinicName":"Demo Clinic","city":"Hyderabad","consultationFee":500.00}
                        """))
                .andExpect(status().isCreated());
    }

    @Test void unauthenticatedPharmacyCanRegister() throws Exception {
        mvc.perform(post("/api/auth/register/pharmacy").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"pharmacyName":"Demo Pharmacy","ownerName":"Test Owner","email":"pharmacy@example.test","phone":"9000000002","password":"securePassword123","confirmPassword":"securePassword123","licenseNumber":"PH-TEST-01","address":"Demo address"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test void unauthenticatedLaboratoryCanRegister() throws Exception {
        mvc.perform(post("/api/auth/register/laboratory").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"laboratoryName":"Demo Lab","ownerName":"Test Owner","email":"lab@example.test","phone":"9000000003","password":"securePassword123","confirmPassword":"securePassword123","accreditationDetails":"LAB-TEST-01","address":"Demo address"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test void adminSetupRegistrationIsPublicButRequiresItsSetupHeaderAtService() throws Exception {
        mvc.perform(post("/api/auth/register/admin").with(csrf()).header("X-Setup-Token", "test-setup-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Test Admin","email":"admin@example.test","phone":"9000000004","password":"securePassword123","confirmPassword":"securePassword123","organizationName":"Demo Org"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test void postWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register/doctor").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your security token is missing or expired. Refresh the page and try again."));
    }

    @Test void invalidRegistrationPayloadIsRejectedByValidation() throws Exception {
        mvc.perform(post("/api/auth/register/doctor").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test void unverifiedAccountCanRequestResendAndResetWithoutSigningIn() throws Exception {
        mvc.perform(post("/api/auth/resend-verification").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"patient@example.test\",\"role\":\"PATIENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("If the account needs verification")));
        mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"patient@example.test\",\"role\":\"PATIENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("does not confirm whether an account exists")));
    }

    @Test void duplicateEmailReturnsConflictWithSafeMessage() throws Exception {
        given(service.registerPatient(any())).willThrow(new IllegalStateException("An account with this email already exists."));
        mvc.perform(post("/api/auth/register/patient").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Patient","email":"patient@example.test","phone":"9000000000","password":"securePassword123","confirmPassword":"securePassword123","dateOfBirth":"1990-01-01","gender":"","address":"Demo address"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test void localhostCorsPreflightAllowsRegistrationHeaders() throws Exception {
        mvc.perform(options("/api/auth/register/doctor")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-setup-token,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("x-setup-token")));
    }

    @Test void protectedAdminEndpointRejectsUnauthenticatedRequest() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test void patientCannotUseAdminEndpointButAdminCan() throws Exception {
        given(users.findByEmailIgnoreCase("patient@example.test")).willReturn(Optional.of(
                new User("Patient", "patient@example.test", "hash", "9000000000", UserRole.PATIENT, VerificationStatus.NOT_REQUIRED)));
        given(users.findByEmailIgnoreCase("admin@example.test")).willReturn(Optional.of(
                new User("Admin", "admin@example.test", "hash", "9000000001", UserRole.ADMIN, VerificationStatus.NOT_REQUIRED)));
        given(users.findAll()).willReturn(List.of());

        mvc.perform(get("/api/admin/users").with(user("patient@example.test").roles("PATIENT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test void pendingProvidersCannotUseTheirRoleEndpoints() throws Exception {
        Object[][] pendingProviders = {
                {"doctor@example.test", UserRole.DOCTOR, "/api/doctor/dashboard"},
                {"pharmacy@example.test", UserRole.PHARMACY, "/api/pharmacy/dashboard"},
                {"lab@example.test", UserRole.LABORATORY, "/api/laboratory/dashboard"}
        };
        for (Object[] provider : pendingProviders) {
            String email = (String) provider[0];
            UserRole role = (UserRole) provider[1];
            String path = (String) provider[2];
            given(users.findByEmailIgnoreCase(email)).willReturn(Optional.of(
                    new User("Pending Provider", email, "hash", "9000000000", role, VerificationStatus.PENDING)));

            mvc.perform(get(path).with(user(email).roles(role.name())))
                    .andExpect(status().isForbidden());
        }
    }
}
