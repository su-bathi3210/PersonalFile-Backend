package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.PasswordChangeRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.RegisterRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.IncrementNotificationResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final Map<String, String> otpCache = new HashMap<>();

    private final EmailService emailService;

    private final NotificationRepository notificationRepository;

    private final DataChangeHistoryRepository historyRepository;

    private final DynamicFieldRepository dynamicFieldRepository;

    private final String UPLOAD_DIR = "uploads/increment-forms/";

    @Override
    public User registerEmployee(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .department(request.getDepartment())
                .roles(Set.of(Role.EMPLOYEE))
                .build();

        return userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    @Transactional
    public void sendIncrementNotification(String userId, List<String> templateNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No employee found."));

        emailService.sendIncrementReminder(user.getEmail(), user.getUsername(), user.getIncrementDate());

        user.setIncrementStatus("EMAIL_SENT");
        userRepository.save(user);

        Notification notification = Notification.builder()
                .userId(userId)
                .message("පාලන අංශය විසින් ඔබගේ වැටුප් වර්ධක පෝරම ඉදිරිපත් කරන ලෙස දන්වා ඇත. කරුණාකර අදාළ ලේඛන බාගත කර පුරවා නැවත උඩුගත කරන්න.")
                .createdAt(LocalDateTime.now())
                .isIncrementType(true)
                .read(false)
                .status("PENDING")
                .originalIncrementDate(user.getIncrementDate())
                .requestedTemplates(templateNames)
                .submittedFileUrls(new ArrayList<>())
                .build();

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void uploadSubmittedForms(String notificationId, List<MultipartFile> files) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found."));

        List<String> savedFilePaths = new ArrayList<>();

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);

                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                savedFilePaths.add("/" + UPLOAD_DIR + fileName);
            }

        } catch (IOException e) {
            throw new RuntimeException("Could not store files. Error: " + e.getMessage());
        }

        notification.setSubmittedFileUrls(savedFilePaths);
        notification.setStatus("SUBMITTED");
        notification.setSubmittedAt(LocalDateTime.now());
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    @Override
    public List<IncrementNotificationResponse> getAllIncrementNotifications() {
        List<Notification> notifications = notificationRepository.findAll();

        return notifications.stream()
                .map(notification -> {
                    User user = userRepository.findById(notification.getUserId()).orElse(null);
                    if (user == null) return null;

                    return IncrementNotificationResponse.builder()
                            .notificationId(notification.getId())
                            .userId(user.getId())
                            .employeeName(user.getUsername())
                            .email(user.getEmail())
                            .phoneNumber(user.getPhoneNumber())
                            .incrementDate(notification.getOriginalIncrementDate())
                            .message(notification.getMessage())
                            .status(notification.getStatus())
                            .sentDate(notification.getCreatedAt())
                            .submittedDate(notification.getSubmittedAt())
                            .requestedTemplates(notification.getRequestedTemplates())
                            .submittedFileUrls(notification.getSubmittedFileUrls())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(IncrementNotificationResponse::getSentDate).reversed())
                .toList();
    }

    @Override
    @Transactional
    public void updatePersonalFile(String id, User updateData, String currentUserEmail, Collection<? extends GrantedAuthority> authorities) {
        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PERSONALFILE_ADMIN"));

        List<DataChangeHistory.FieldChange> fieldChanges = new ArrayList<>();

        compareAndAdd(fieldChanges, "username", userToUpdate.getUsername(), updateData.getUsername());
        compareAndAdd(fieldChanges, "email", userToUpdate.getEmail(), updateData.getEmail());
        compareAndAdd(fieldChanges, "nic", userToUpdate.getNic(), updateData.getNic());
        compareAndAdd(fieldChanges, "address", userToUpdate.getAddress(), updateData.getAddress());
        compareAndAdd(fieldChanges, "phoneNumber", userToUpdate.getPhoneNumber(), updateData.getPhoneNumber());
        compareAndAdd(fieldChanges, "profileImage", userToUpdate.getProfileImage(), updateData.getProfileImage());
        compareAndAdd(fieldChanges, "gender", userToUpdate.getGender(), updateData.getGender());
        compareAndAdd(fieldChanges, "dateOfBirth",
                userToUpdate.getDateOfBirth() != null ? userToUpdate.getDateOfBirth().toString() : null,
                updateData.getDateOfBirth() != null ? updateData.getDateOfBirth().toString() : null);

        if (isAdmin) {
            compareAndAdd(fieldChanges, "wnopNumber", userToUpdate.getWnopNumber(), updateData.getWnopNumber());
            compareAndAdd(fieldChanges, "serviceNumber", userToUpdate.getServiceNumber(), updateData.getServiceNumber());
            compareAndAdd(fieldChanges, "department", userToUpdate.getDepartment(), updateData.getDepartment());
            compareAndAdd(fieldChanges, "designation", userToUpdate.getDesignation(), updateData.getDesignation());
            compareAndAdd(fieldChanges, "dutyPlace", userToUpdate.getDutyPlace(), updateData.getDutyPlace());
            compareAndAdd(fieldChanges, "grade", userToUpdate.getGrade(), updateData.getGrade());
            compareAndAdd(fieldChanges, "salaryScale", userToUpdate.getSalaryScale(), updateData.getSalaryScale());

            compareAndAdd(fieldChanges, "dateOfLanguageProficiency",
                    userToUpdate.getDateOfLanguageProficiency() != null ? userToUpdate.getDateOfLanguageProficiency().toString() : null,
                    updateData.getDateOfLanguageProficiency() != null ? updateData.getDateOfLanguageProficiency().toString() : null);

            compareAndAdd(fieldChanges, "dateOfFirstAppointment",
                    userToUpdate.getDateOfFirstAppointment() != null ? userToUpdate.getDateOfFirstAppointment().toString() : null,
                    updateData.getDateOfFirstAppointment() != null ? updateData.getDateOfFirstAppointment().toString() : null);

            compareAndAdd(fieldChanges, "appointmentDateToPresentStatus",
                    userToUpdate.getAppointmentDateToPresentStatus() != null ? userToUpdate.getAppointmentDateToPresentStatus().toString() : null,
                    updateData.getAppointmentDateToPresentStatus() != null ? updateData.getAppointmentDateToPresentStatus().toString() : null);

            compareAndAdd(fieldChanges, "incrementDate",
                    userToUpdate.getIncrementDate() != null ? userToUpdate.getIncrementDate().toString() : null,
                    updateData.getIncrementDate() != null ? updateData.getIncrementDate().toString() : null);

            compareAndAdd(fieldChanges, "dateOfReceiptOfRelevantGrade",
                    userToUpdate.getDateOfReceiptOfRelevantGrade() != null ? userToUpdate.getDateOfReceiptOfRelevantGrade().toString() : null,
                    updateData.getDateOfReceiptOfRelevantGrade() != null ? updateData.getDateOfReceiptOfRelevantGrade().toString() : null);

            compareAndAdd(fieldChanges, "dateOfCompulsoryRetirement",
                    userToUpdate.getDateOfCompulsoryRetirement() != null ? userToUpdate.getDateOfCompulsoryRetirement().toString() : null,
                    updateData.getDateOfCompulsoryRetirement() != null ? updateData.getDateOfCompulsoryRetirement().toString() : null);

            compareAndAdd(fieldChanges, "presentStatusDate",
                    userToUpdate.getPresentStatusDate() != null ? userToUpdate.getPresentStatusDate().toString() : null,
                    updateData.getPresentStatusDate() != null ? updateData.getPresentStatusDate().toString() : null);

            compareAndAdd(fieldChanges, "dateOfReceiptGradeI",
                    userToUpdate.getDateOfReceiptGradeI() != null ? userToUpdate.getDateOfReceiptGradeI().toString() : null,
                    updateData.getDateOfReceiptGradeI() != null ? updateData.getDateOfReceiptGradeI().toString() : null);

            compareAndAdd(fieldChanges, "dateOfReceiptGradeII",
                    userToUpdate.getDateOfReceiptGradeII() != null ? userToUpdate.getDateOfReceiptGradeII().toString() : null,
                    updateData.getDateOfReceiptGradeII() != null ? updateData.getDateOfReceiptGradeII().toString() : null);

            compareAndAdd(fieldChanges, "dateOfReceiptGradeIII",
                    userToUpdate.getDateOfReceiptGradeIII() != null ? userToUpdate.getDateOfReceiptGradeIII().toString() : null,
                    updateData.getDateOfReceiptGradeIII() != null ? updateData.getDateOfReceiptGradeIII().toString() : null);
        }

        if (updateData.getDynamicFields() != null) {
            if (userToUpdate.getDynamicFields() == null) {
                userToUpdate.setDynamicFields(new HashMap<>());
            }

            List<DynamicField> configs = dynamicFieldRepository.findAll();
            Map<String, DynamicField> configMap = new HashMap<>();
            for (DynamicField c : configs) {
                configMap.put(c.getFieldKey(), c);
            }

            for (Map.Entry<String, Object> entry : updateData.getDynamicFields().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                String oldVal = userToUpdate.getDynamicFields().get(key) != null ? userToUpdate.getDynamicFields().get(key).toString() : "";
                String newVal = value != null ? value.toString() : "";

                if (!oldVal.trim().equals(newVal.trim())) {
                    DynamicField config = configMap.get(key);
                    if (config != null && !isAdmin && config.isAdminOnly()) {
                        throw new RuntimeException("Error: You do not have permission to update the field: " + config.getDisplayName());
                    }
                    compareAndAdd(fieldChanges, "dynamicFields." + key, oldVal, newVal);
                }
            }
        }

        if (!fieldChanges.isEmpty()) {
            long currentCount = historyRepository.countByUserId(id);
            int nextRevision = (int) currentCount + 1;
            String roleDisplay = isAdmin ? "PERSONALFILE_ADMIN" : "EMPLOYEE";

            DataChangeHistory historyEntry = DataChangeHistory.builder()
                    .userId(id)
                    .employeeName(userToUpdate.getUsername())
                    .changedBy(roleDisplay)
                    .changedAt(LocalDateTime.now())
                    .revisionNumber(nextRevision)
                    .changes(fieldChanges)
                    .build();

            historyRepository.save(historyEntry);

            if (!isAdmin) {
                Notification adminNotification = Notification.builder()
                        .userId(id)
                        .message(userToUpdate.getUsername() + " Personal Details has been revised.")
                        .createdAt(LocalDateTime.now())
                        .isIncrementType(false)
                        .read(false)
                        .status("PROFILE_UPDATED")
                        .build();

                notificationRepository.save(adminNotification);
            }
        }

        userToUpdate.setUsername(updateData.getUsername());
        userToUpdate.setEmail(updateData.getEmail());
        userToUpdate.setNic(updateData.getNic());
        userToUpdate.setAddress(updateData.getAddress());
        userToUpdate.setPhoneNumber(updateData.getPhoneNumber());
        userToUpdate.setProfileImage(updateData.getProfileImage());
        userToUpdate.setGender(updateData.getGender());
        userToUpdate.setDateOfBirth(updateData.getDateOfBirth());

        if (isAdmin) {
            userToUpdate.setWnopNumber(updateData.getWnopNumber());
            userToUpdate.setServiceNumber(updateData.getServiceNumber());
            userToUpdate.setDepartment(updateData.getDepartment());
            userToUpdate.setDesignation(updateData.getDesignation());
            userToUpdate.setDutyPlace(updateData.getDutyPlace());
            userToUpdate.setGrade(updateData.getGrade());
            userToUpdate.setDateOfLanguageProficiency(updateData.getDateOfLanguageProficiency());
            userToUpdate.setSalaryScale(updateData.getSalaryScale());
            userToUpdate.setDateOfFirstAppointment(updateData.getDateOfFirstAppointment());
            userToUpdate.setAppointmentDateToPresentStatus(updateData.getAppointmentDateToPresentStatus());
            userToUpdate.setIncrementDate(updateData.getIncrementDate());
            userToUpdate.setDateOfReceiptOfRelevantGrade(updateData.getDateOfReceiptOfRelevantGrade());
            userToUpdate.setDateOfCompulsoryRetirement(updateData.getDateOfCompulsoryRetirement());

            userToUpdate.setPresentStatusDate(updateData.getPresentStatusDate());
            userToUpdate.setDateOfReceiptGradeI(updateData.getDateOfReceiptGradeI());
            userToUpdate.setDateOfReceiptGradeII(updateData.getDateOfReceiptGradeII());
            userToUpdate.setDateOfReceiptGradeIII(updateData.getDateOfReceiptGradeIII());
        }

        if (updateData.getDynamicFields() != null) {
            if (userToUpdate.getDynamicFields() == null) {
                userToUpdate.setDynamicFields(new HashMap<>());
            }
            userToUpdate.getDynamicFields().putAll(updateData.getDynamicFields());
        }

        userRepository.save(userToUpdate);
    }

    @Override
    @Transactional
    public void resolveProfileUpdateNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        List<Notification> activeNotifications = notificationRepository.findByUserId(user.getId())
                .stream()
                .filter(n -> "PROFILE_UPDATED".equals(n.getStatus()))
                .toList();

        for (Notification notification : activeNotifications) {
            notification.setStatus("RESOLVED");
            notification.setRead(true);
        }

        notificationRepository.saveAll(activeNotifications);
    }

    private void compareAndAdd(List<DataChangeHistory.FieldChange> list, String fieldName, String oldVal, String newVal) {
        String actualOld = oldVal == null ? "" : oldVal.trim();
        String actualNew = newVal == null ? "" : newVal.trim();

        if (!actualOld.equals(actualNew)) {
            list.add(new DataChangeHistory.FieldChange(fieldName, actualOld, actualNew));
        }
    }

    @Override
    public List<DataChangeHistory> getUserHistoryByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return historyRepository.findByUserIdOrderByChangedAtDesc(user.getId());
    }

    @Override
    public long getChangeCountByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return historyRepository.countByUserId(user.getId());
    }

    @Override
    public List<DataChangeHistory> getUserHistory(String userId) {
        return historyRepository.findByUserIdOrderByChangedAtDesc(userId);
    }

    @Override
    public long getChangeCount(String userId) {
        return historyRepository.countByUserId(userId);
    }

    public List<User> getAllEmployeesOnly() {
        return userRepository.findByRolesContaining(Role.EMPLOYEE);
    }

    @Override
    public void changePassword(String email, PasswordChangeRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void deleteUserById(String id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteUsersByIds(List<String> ids) {
        userRepository.deleteAllById(ids);
    }

    @Override
    @Transactional
    public User createEmployeeByAdmin(User userDetails) {
        if (userRepository.existsByUsername(userDetails.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(userDetails.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }
        userDetails.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        userDetails.setRoles(Set.of(Role.EMPLOYEE));
        return userRepository.save(userDetails);
    }

    @Override
    public void processForgotPassword(String email, String serviceNumber) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email"));

        if (!serviceNumber.equals(user.getServiceNumber())) {
            throw new RuntimeException("Service Number does not match!");
        }

        String otp = String.valueOf(new Random().nextInt(899999) + 100000);
        otpCache.put(email, otp);

        emailService.sendOTP(email, otp);
    }

    @Override
    public boolean verifyOTP(String email, String otp) {
        return otpCache.containsKey(email) && otpCache.get(email).equals(otp);
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpCache.remove(email);
    }

    @Override
    @Transactional
    public void updateNextIncrementDate(String userId, String nextIncrementDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        int currentYear = java.time.LocalDate.now().getYear();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        java.time.LocalDate parsedDate = java.time.LocalDate.parse(currentYear + "-" + nextIncrementDate, formatter);

        user.setIncrementDate(parsedDate);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void approveIncrementNotification(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found."));

        if (!"SUBMITTED".equals(notification.getStatus())) {
            throw new RuntimeException("Only submitted forms can be approved.");
        }

        User user = userRepository.findById(notification.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (user.getIncrementDate() != null) {
            java.time.LocalDate nextYearIncrement = user.getIncrementDate().plusYears(1);
            user.setIncrementDate(nextYearIncrement);
        }

        user.setIncrementStatus("APPROVED");
        userRepository.save(user);

        notification.setStatus("APPROVED");
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}