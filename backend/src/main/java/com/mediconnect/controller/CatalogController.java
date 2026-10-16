package com.mediconnect.controller;
import com.mediconnect.enums.UserRole;import com.mediconnect.repository.UserRepository;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController public class CatalogController{
 private final UserRepository users;public CatalogController(UserRepository users){this.users=users;}
 @GetMapping("/api/labs") public List<?> labs(){return users.findAll().stream().filter(u->u.getRole()==UserRole.LABORATORY&&u.getVerificationStatus()==com.mediconnect.enums.VerificationStatus.APPROVED&&u.isActive()).map(u->Map.of("id",u.getId().toString(),"name",u.getName(),"email",u.getEmail())).toList();}
 @GetMapping("/api/labs/{id}/tests") public List<?> tests(@PathVariable UUID id){if(users.findById(id).filter(u->u.getRole()==UserRole.LABORATORY&&u.getVerificationStatus()==com.mediconnect.enums.VerificationStatus.APPROVED).isEmpty())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);return List.of();}
 @GetMapping("/api/medicines") public List<?> medicines(){return List.of();}
 @GetMapping("/api/medicines/{id}") public Object medicine(@PathVariable UUID id){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);}
}
