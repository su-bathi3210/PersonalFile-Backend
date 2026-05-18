package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.IncrementNotificationResponse;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.PasswordChangeRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.RegisterRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public interface UserService {
    User registerEmployee(RegisterRequest request);
    User findByEmail(String email);
    List<User> getAllEmployeesOnly();
    void changePassword(String email, PasswordChangeRequest request);
    void deleteUserById(String id);
    void deleteUsersByIds(List<String> ids);
    User createEmployeeByAdmin(User userDetails);
    void processForgotPassword(String email, String serviceNumber);
    boolean verifyOTP(String email, String otp);
    void resetPassword(String email, String newPassword);
    void sendIncrementNotification(String userId);
    List<IncrementNotificationResponse> getAllIncrementNotifications();
    void updateNextIncrementDate(String userId, String nextIncrementDate);
    void updatePersonalFile(String email, User updateData, String currentUserEmail, Collection<? extends GrantedAuthority> authorities);
}