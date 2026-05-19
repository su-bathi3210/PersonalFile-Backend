package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.PasswordChangeRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.RegisterRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.DataChangeHistoryRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.IncrementFormRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.NotificationRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.UserRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.IncrementNotificationResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final IncrementFormRepository incrementFormRepository;

    private final DataChangeHistoryRepository historyRepository;

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

            userRepository.save(userToUpdate);
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
    public void sendIncrementNotification(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No employee found."));

        emailService.sendIncrementReminder(user.getEmail(), user.getUsername(), user.getIncrementDate());

        user.setIncrementStatus("EMAIL_SENT");
        userRepository.save(user);

        Notification notification = Notification.builder()
                .userId(id)
                .message("පාලන අංශය විසින් ඔබගේ වැටුප් වර්ධක පෝරමය ඉදිරිපත් කරන ලෙස දන්වා ඇත.")
                .createdAt(LocalDateTime.now())
                .isIncrementType(true)
                .read(false)
                .status("PENDING")
                .originalIncrementDate(user.getIncrementDate())
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public List<IncrementNotificationResponse> getAllIncrementNotifications() {
        List<Notification> notifications = notificationRepository.findAll();

        return notifications.stream()
                .map(notification -> {
                    User user = userRepository.findById(notification.getUserId())
                            .orElse(null);

                    if (user == null) return null;
                    IncrementForm form = incrementFormRepository.findAll().stream()
                            .filter(f -> notification.getId().equals(f.getNotificationId()))
                            .findFirst()
                            .orElse(null);

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
                            .submittedDate(form != null ? notification.getCreatedAt() : null)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(IncrementNotificationResponse::getSentDate).reversed())
                .toList();
    }

    @Transactional
    public void updateNextIncrementDate(String userId, String nextIncrementDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setIncrementDate(java.time.LocalDate.parse(nextIncrementDate));
        userRepository.save(user);
    }
}