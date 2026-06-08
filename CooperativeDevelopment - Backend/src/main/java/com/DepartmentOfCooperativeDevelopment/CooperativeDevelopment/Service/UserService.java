package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.IncrementNotificationResponse;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.PasswordChangeRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.RegisterRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.DataChangeHistory;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.multipart.MultipartFile;

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
    List<IncrementNotificationResponse> getAllIncrementNotifications();
    void updateNextIncrementDate(String userId, String nextIncrementDate);
    List<DataChangeHistory> getUserHistory(String userId);
    long getChangeCount(String userId);
    List<DataChangeHistory> getUserHistoryByEmail(String email);
    long getChangeCountByEmail(String email);
    void resolveProfileUpdateNotifications(String email);
    void sendIncrementNotification(String userId, List<String> templateNames);
    void uploadSubmittedForms(String notificationId, List<MultipartFile> files);
    void approveIncrementNotification(String notificationId);
    void updatePersonalFile(String email, User updateData, String currentUserEmail, Collection<? extends GrantedAuthority> authorities);
}