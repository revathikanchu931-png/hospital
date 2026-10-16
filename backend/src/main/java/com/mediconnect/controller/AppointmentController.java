package com.mediconnect.controller;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mediconnect.enums.AppointmentStatus;
import com.mediconnect.service.AppointmentService;
import com.mediconnect.service.UserService;
@RestController @RequestMapping("/api/appointments") public class AppointmentController{
 private final AppointmentService appointments;private final UserService users;public AppointmentController(AppointmentService appointments,UserService users){this.appointments=appointments;this.users=users;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Object book(@RequestBody BookingRequest body,Authentication auth){return appointments.book(users.current(auth),body.doctorId(),body.availabilityId());}
 @GetMapping("/my") public List<?> mine(Authentication auth){return appointments.mine(users.current(auth));}
 @PatchMapping("/{id}/cancel") public Object cancel(@PathVariable UUID id,Authentication auth){return appointments.cancel(users.current(auth),id);}
 @PutMapping("/{id}/reschedule") public Object reschedule(@PathVariable UUID id,@RequestBody BookingRequest body,Authentication auth){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_IMPLEMENTED,"Rescheduling is not available in this release.");}
 @PatchMapping("/{id}/status") public Object status(@PathVariable UUID id,@RequestBody StatusRequest body,Authentication auth){return appointments.updateStatus(users.current(auth),id,body.status());}
 public record BookingRequest(UUID doctorId,UUID availabilityId){}public record StatusRequest(AppointmentStatus status){}
}
