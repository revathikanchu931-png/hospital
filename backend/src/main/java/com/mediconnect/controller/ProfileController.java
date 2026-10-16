package com.mediconnect.controller;
import com.mediconnect.dto.AuthDtos.ProfileUpdate;import com.mediconnect.service.UserService;import jakarta.validation.Valid;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @RequestMapping("/api/profile") public class ProfileController{
 private final UserService users;public ProfileController(UserService users){this.users=users;}
 @GetMapping("/me") public Map<String,Object> me(Authentication auth){return users.profile(users.current(auth));}
 @PutMapping("/me") public Map<String,Object> update(@Valid @RequestBody ProfileUpdate request,Authentication auth){return users.updateProfile(users.current(auth),request);}
}
