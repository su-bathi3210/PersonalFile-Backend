package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.IncrementUpdateDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.IncrementForm;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Notification;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.IncrementFormRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.NotificationRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.UserRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.ExcelService;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/personalfile")
@RequiredArgsConstructor
public class PersonalFileController {

    private final UserService userService;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final ExcelService excelService;

    private final NotificationRepository notificationRepository;

    private final IncrementFormRepository incrementFormRepository;

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@RequestParam String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    @PutMapping("/update-profile")
    @PreAuthorize("hasAnyRole('PERSONALFILE_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<String> updateProfile(
            @RequestParam String id,
            @RequestBody User updateData,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String currentUserEmail = authentication.getName();
        var authorities = authentication.getAuthorities();

        userService.updatePersonalFile(id, updateData, currentUserEmail, authorities);
        return ResponseEntity.ok("Profile Updated Successfully!");
    }

    @GetMapping("/all-employees")
    public ResponseEntity<?> getAllEmployees() {
        return ResponseEntity.ok(userService.getAllEmployeesOnly());
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> deleteEmployee(@PathVariable String id) {
        userService.deleteUserById(id);
        return ResponseEntity.ok("Employee deleted successfully!");
    }

    @PostMapping("/delete-multiple")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> deleteMultipleEmployees(@RequestBody List<String> ids) {
        userService.deleteUsersByIds(ids);
        return ResponseEntity.ok("Selected employees deleted successfully!");
    }

    @PostMapping("/add-employee")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> addEmployee(@RequestBody User newUserDetails) {
        try {
            User savedUser = userService.createEmployeeByAdmin(newUserDetails);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating employee: " + e.getMessage());
        }
    }

    @PostMapping("/upload-employees")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> uploadEmployees(@RequestParam("file") MultipartFile file) {
 
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select an Excel file.");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                && !contentType.equals("application/vnd.ms-excel"))) {
            return ResponseEntity.badRequest().body("Invalid file type. Please upload a .xlsx or .xls file.");
        }

        try {
            List<User> users = excelService.parseExcel(file.getInputStream(), passwordEncoder);

            if (users.isEmpty()) {
                return ResponseEntity.badRequest().body("No data was found in the Excel file.");
            }
            userRepository.saveAll(users);

            return ResponseEntity.ok("Employees " + users.size() + " People were successfully added to the system.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Data entry failed: " + e.getMessage());
        }
    }

    @PostMapping("/send-increment-email/{id}")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> sendIncrementEmail(@PathVariable String id) {
        try {
            userService.sendIncrementNotification(id);
            return ResponseEntity.ok("The email was sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/notifications/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(notificationRepository.findByUserId(userId));
    }

    @PutMapping("/notifications/read/{id}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable String id) {

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);

        notificationRepository.save(notification);

        return ResponseEntity.ok("Notification marked as read");
    }

    @GetMapping("/notifications/all")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    @GetMapping("/increment-notifications")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> getIncrementNotifications() {
        return ResponseEntity.ok(userService.getAllIncrementNotifications());
    }

    @PutMapping("/update-increment-date/{userId}")
    public ResponseEntity<?> updateIncrementDate(@PathVariable String userId, @RequestBody IncrementUpdateDTO dto) {
        userService.updateNextIncrementDate(userId, dto.getNextIncrementDate());
        return ResponseEntity.ok("Increment date updated successfully");
    }
}