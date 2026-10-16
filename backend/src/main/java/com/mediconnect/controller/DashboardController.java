package com.mediconnect.controller;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mediconnect.enums.UserRole;
import com.mediconnect.repository.AppointmentRepository;
import com.mediconnect.repository.UserRepository;
import com.mediconnect.service.UserService;
@RestController public class DashboardController{
 private final UserService users;private final UserRepository accounts;private final AppointmentRepository appointments;public DashboardController(UserService users,UserRepository accounts,AppointmentRepository appointments){this.users=users;this.accounts=accounts;this.appointments=appointments;}
 @GetMapping("/api/dashboard") public Map<String,Object> dashboard(Authentication auth){var u=users.current(auth);return Map.of("role",u.getRole().name(),"name",u.getName(),"metrics",Map.of("appointments",appointments.countByStatus(com.mediconnect.enums.AppointmentStatus.PENDING),"patients",accounts.countActiveByRole(UserRole.PATIENT),"doctors",accounts.countActiveByRole(UserRole.DOCTOR),"laboratories",accounts.countActiveByRole(UserRole.LABORATORY),"pharmacies",accounts.countActiveByRole(UserRole.PHARMACY)));}
 @GetMapping("/api/admin/dashboard") public Map<String,Object> admin(Authentication auth){users.current(auth);return Map.of("metrics",Map.of("patients",accounts.countActiveByRole(UserRole.PATIENT),"doctors",accounts.countActiveByRole(UserRole.DOCTOR),"laboratories",accounts.countActiveByRole(UserRole.LABORATORY),"pharmacies",accounts.countActiveByRole(UserRole.PHARMACY),"appointments",appointments.count(),"pendingProviders",accounts.countPending(UserRole.DOCTOR)+accounts.countPending(UserRole.LABORATORY)+accounts.countPending(UserRole.PHARMACY)));}
 @GetMapping("/api/admin/users") public List<?> listUsers(){return accounts.findAll().stream().map(u->Map.of("id",u.getId().toString(),"name",u.getName(),"email",u.getEmail(),"role",u.getRole().name(),"active",u.isActive(),"verificationStatus",u.getVerificationStatus().name())).toList();}
 @PostMapping("/api/admin/users") @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Object createProvider(@RequestBody com.mediconnect.dto.AuthDtos.ProviderCreateRequest body,Authentication auth){return users.createProvider(users.current(auth),body);}
 @PatchMapping("/api/admin/users/{id}/activation") public Object activation(@PathVariable UUID id,@RequestBody com.mediconnect.dto.AuthDtos.ActivationRequest body,Authentication auth){return Map.of("id",id.toString(),"active",users.setActive(users.current(auth),id,body.active()));}
 @GetMapping("/api/admin/providers") public List<?> providers(){return accounts.findPendingProviders(List.of(UserRole.DOCTOR,UserRole.LABORATORY,UserRole.PHARMACY)).stream().map(u->Map.of("id",u.getId().toString(),"name",u.getName(),"email",u.getEmail(),"role",u.getRole().name(),"verificationStatus",u.getVerificationStatus().name())).toList();}
 @PatchMapping("/api/admin/providers/{id}/verification") public Object verify(@PathVariable UUID id,@RequestBody com.mediconnect.dto.AuthDtos.VerificationRequest request,Authentication auth){return users.verifyProvider(users.current(auth),id,request.approved());}
}
