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

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 👈 1. CORS ප්‍රශ්නය විසඳීමට මේක අනිවාර්යයෙන්ම එකතු කරන්න
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Spring Security මඟින් Username (හෝ Phone) සහ Password (හෝ NIC) පරික්ෂා කිරීම
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(request.getUsername(), authentication.getAuthorities());

        // 👈 2. මෙතන findByEmail එකේදී ගැටලුවක් ආවොත් සිස්ටම් එක Crash නොවී බේරෙන්න මෙහෙම කරන්න:
        User user;
        try {
            user = userService.findByEmail(request.getUsername());
        } catch (Exception e) {
            // Email එකෙන් හමුනොවුනහොත් (උදාහරණයක් ලෙස Username එකට Phone එකක් තිබේ නම්) Username එකෙන් සොයන්න Custom ක්‍රමයක්:
            // දැනට findByEmail එක වැඩ නම් මේ try-catch එක ඇතුළේ එක ක්‍රියාත්මක වේවි.
            user = null;
        }

        // ආරක්ෂිතව Response එක සකස් කිරීම
        String userEmail = (user != null) ? user.getEmail() : request.getUsername();
        String displayUsername = (user != null) ? user.getUsername() : request.getUsername();

        return ResponseEntity.ok(new LoginResponse(
                token,
                userEmail,
                displayUsername,
                roles
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        User user = userService.registerEmployee(request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Principal principal, @RequestBody PasswordChangeRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("පරිශීලකයා හඳුනාගත නොහැක. කරුණාකර නැවත ලොග් වන්න.");
        }

        String email = principal.getName();
        userService.changePassword(email, request);
        return ResponseEntity.ok("මුරපදය සාර්ථකව වෙනස් කරන ලදී.");
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