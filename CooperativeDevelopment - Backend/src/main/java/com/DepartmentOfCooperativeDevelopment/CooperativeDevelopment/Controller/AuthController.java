package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.UserService;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Config.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(request.getUsername(), authentication.getAuthorities());

        User user = userService.findByEmail(request.getUsername());

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getEmail(),
                user.getUsername(),
                roles
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {

        User user = userService.registerEmployee(request);

        return ResponseEntity.ok(user);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody PasswordChangeRequest request,
            Authentication authentication) {

        String email = authentication.getName(); 
        userService.changePassword(email, request);

        return ResponseEntity.ok("Password changed successfully");
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> requestOTP(@RequestBody ForgotRequest request) {
        userService.processForgotPassword(request.getEmail(), request.getServiceNumber());
        return ResponseEntity.ok("OTP sent to your email.");
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifyOTP(@RequestBody ResetPasswordRequest request) {
        boolean isValid = userService.verifyOTP(request.getEmail(), request.getOtp());
        if (isValid) {
            return ResponseEntity.ok("OTP verified successfully.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP.");
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok("Password reset successfully.");
    }
}