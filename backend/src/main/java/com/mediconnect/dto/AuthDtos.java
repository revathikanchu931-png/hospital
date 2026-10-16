package com.mediconnect.dto;
import com.mediconnect.enums.UserRole;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.LocalDate;
public final class AuthDtos{private AuthDtos(){}
 public record RegisterRequest(@NotBlank @Size(min=2,max=140)String name,@NotBlank @Email @Size(max=190)String email,@NotBlank @Size(max=30)String phone,@NotBlank @Size(min=8,max=72)String password,@NotBlank String confirmPassword,@NotNull @Past LocalDate dateOfBirth,@Size(max=30)String gender,@NotBlank @Size(max=500)String address){}
 public record DoctorRegistration(@NotBlank @Size(min=2,max=140)String name,@NotBlank @Email @Size(max=190)String email,@NotBlank @Size(max=30)String phone,@NotBlank @Size(min=12,max=72)String password,@NotBlank String confirmPassword,@NotBlank @Size(max=100)String registrationNumber,@NotBlank @Size(max=200)String qualification,@NotBlank @Size(max=120)String specialization,@Min(0) @Max(70)int experienceYears,@NotBlank @Size(max=200)String clinicName,@NotBlank @Size(max=100)String city,@NotNull @DecimalMin("0.00")BigDecimal consultationFee){}
 public record PharmacyRegistration(@NotBlank @Size(min=2,max=140)String pharmacyName,@NotBlank @Size(min=2,max=140)String ownerName,@NotBlank @Email @Size(max=190)String email,@NotBlank @Size(max=30)String phone,@NotBlank @Size(min=12,max=72)String password,@NotBlank String confirmPassword,@NotBlank @Size(max=120)String licenseNumber,@NotBlank @Size(max=500)String address){}
 public record LaboratoryRegistration(@NotBlank @Size(min=2,max=140)String laboratoryName,@NotBlank @Size(min=2,max=140)String ownerName,@NotBlank @Email @Size(max=190)String email,@NotBlank @Size(max=30)String phone,@NotBlank @Size(min=12,max=72)String password,@NotBlank String confirmPassword,@NotBlank @Size(max=200)String accreditationDetails,@NotBlank @Size(max=500)String address){}
 public record SetupAdminRequest(@NotBlank @Size(min=2,max=140)String name,@NotBlank @Email String email,@Size(max=30)String phone,@NotBlank @Size(min=12,max=72)String password,@NotBlank String confirmPassword,@NotBlank @Size(max=200)String organizationName){}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank String password,@NotNull UserRole role){}
 public record UserResponse(String id,String name,String email,String phone,String role,String verificationStatus,String accountStatus,boolean emailVerified,Boolean verificationEmailSent){public UserResponse(String id,String name,String email,String phone,String role,String verificationStatus,String accountStatus,boolean emailVerified){this(id,name,email,phone,role,verificationStatus,accountStatus,emailVerified,null);}}
 public record TokenRequest(@NotBlank String token){}
 public record AccountEmailRequest(@NotBlank @Email String email,@NotNull UserRole role){}
 public record PasswordResetRequest(@NotBlank @Email String email,@NotNull UserRole role){}
 public record PasswordResetConfirm(@NotBlank String token,@NotBlank @Size(min=12,max=72)String password,@NotBlank String confirmPassword){}
 public record ProfileUpdate(@NotBlank @Size(min=2,max=140)String name,@Size(max=30)String phone){}
 public record ProviderCreateRequest(@NotBlank @Size(min=2,max=140)String name,@NotBlank @Email String email,@Size(max=30)String phone,@NotBlank @Size(min=12,max=72)String password,@NotNull UserRole role){}
 public record VerificationRequest(boolean approved){}public record ActivationRequest(boolean active){}
}
