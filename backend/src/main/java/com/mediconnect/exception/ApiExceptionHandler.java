package com.mediconnect.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Locale;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> conflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> dataConflict(DataIntegrityViolationException exception) {
        String constraint = exceptionChain(exception).toLowerCase(Locale.ROOT);
        String message = constraint.contains("uk_users_email")
                ? "This email is already registered. Sign in or use a different email."
                : constraint.contains("uk_doctor_registration")
                    ? "This professional registration number is already in use."
                    : constraint.contains("uk_lab_license")
                        || constraint.contains("uk_pharmacy_license")
                    ? "This provider license number is already in use."
                    : "Some submitted information is already in use. Check the email and registration or license number.";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", message));
    }

    private String exceptionChain(Throwable exception) {
        StringBuilder messages = new StringBuilder();
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null) {
                messages.append(' ').append(cause.getMessage());
            }
        }
        return messages.toString();
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<?> denied(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid email or password."));
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    ResponseEntity<?> disabled(org.springframework.security.authentication.DisabledException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(com.mediconnect.service.UserService.EmailDeliveryUnavailableException.class)
    ResponseEntity<?> emailUnavailable(com.mediconnect.service.UserService.EmailDeliveryUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
    ResponseEntity<?> pending(org.springframework.security.authentication.LockedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .toList();
        String message = errors.isEmpty()
                ? "Please check the submitted information."
                : "Please correct the following: " + String.join("; ", errors) + ".";
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException exception) {
        String reason = exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", reason == null ? "The request could not be completed." : reason));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<?> forbidden(org.springframework.security.access.AccessDeniedException exception) {
        String message = exception.getMessage() == null
                ? "You are not authorized to perform this action."
                : exception.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", message));
    }
}
