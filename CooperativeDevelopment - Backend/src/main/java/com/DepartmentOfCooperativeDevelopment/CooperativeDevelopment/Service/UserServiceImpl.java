package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.PasswordChangeRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.RegisterRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Leave.LeaveEntitlement;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.IncrementNotificationResponse;

import jakarta.mail.internet.MimeMessage;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

    private final DesignationTemplateMappingRepository designationTemplateMappingRepository;

    private final String UPLOAD_DIR = "uploads/increment-forms/";

    @Autowired
    @Qualifier("leaveMongoTemplate")
    private MongoTemplate leaveTemplate;

    @Override
    public List<LeaveEntitlement> getSickLeaveEntitlements(String email, int year) {
        Query query = new Query();
        String sanitizedEmail = (email != null) ? email.trim().toLowerCase() : "";

        Criteria yearCriteria = new Criteria().orOperator(
                Criteria.where("year").is(year),
                Criteria.where("year").is(String.valueOf(year))
        );

        query.addCriteria(Criteria.where("employeeEmail").is(sanitizedEmail)
                .and("leaveType").is("SICK")
                .andOperator(yearCriteria));

        List<Document> rawDocuments = leaveTemplate.find(query, Document.class, "Leave Entitlements");

        return rawDocuments.stream().map(doc -> {
            double usedDaysValue = 0.0;
            if (doc.containsKey("usedDays")) {
                Object obj = doc.get("usedDays");
                if (obj instanceof Number) {
                    usedDaysValue = ((Number) obj).doubleValue();
                }
            }

            return LeaveEntitlement.builder()
                    .employeeEmail(doc.getString("employeeEmail"))
                    .leaveType(doc.getString("leaveType"))
                    .usedDays(usedDaysValue)
                    .year(year)
                    .build();
        }).toList();
    }

    @Override
    public User registerEmployee(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
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
    public void sendIncrementNotification(String employeeId, List<String> templateNames) {
        try {
            User employee = userRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("The employee was not found."));

            int currentYear = 2026;
            int oldYear = currentYear - 1;
            String employeeEmail = employee.getEmail() != null ? employee.getEmail().trim().toLowerCase() : "";

            double currentSickUsed = 0.0;
            List<LeaveEntitlement> currentEntitlements = getSickLeaveEntitlements(employeeEmail, currentYear);
            if (currentEntitlements != null && !currentEntitlements.isEmpty()) {
                currentSickUsed = currentEntitlements.get(0).getUsedDays();
            }

            double oldSickUsed = 0.0;
            List<LeaveEntitlement> oldEntitlements = getSickLeaveEntitlements(employeeEmail, oldYear);
            if (oldEntitlements != null && !oldEntitlements.isEmpty()) {
                oldSickUsed = oldEntitlements.get(0).getUsedDays();
            }

            double incYearSickUsed = calculateSickLeaveForIncrementYear(employeeEmail, employee.getIncrementDate());

            List<String> generatedFilePaths = new ArrayList<>();
            List<String> generatedFileNames = new ArrayList<>();

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (String templateName : templateNames) {
                String outputFileName = System.currentTimeMillis() + "_" + employee.getUsername() + "_" + templateName;
                Path targetPath = uploadPath.resolve(outputFileName);

                org.springframework.core.io.ClassPathResource resource =
                        new org.springframework.core.io.ClassPathResource("templates/increment/" + templateName);

                if (resource.exists()) {
                    try (java.io.InputStream is = resource.getInputStream()) {
                        generateAutoFilledDocxWithLeave(is, targetPath, employee, oldSickUsed, currentSickUsed, incYearSickUsed);
                    }
                    generatedFilePaths.add(targetPath.toString());
                    generatedFileNames.add("/" + UPLOAD_DIR + outputFileName);
                }
            }

            Notification notification = new Notification();
            notification.setUserId(employee.getId());
            notification.setMessage("Your salary increment forms are ready. Please download, fill in and resubmit.");
            notification.setStatus("PENDING");
            notification.setIncrementType(true);

            notification.setGeneratedFileUrls(generatedFileNames);
            notification.setSubmittedFileUrls(new ArrayList<>());

            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);

            emailService.sendIncrementEmailWithAttachments(employee.getEmail(), employee.getUsername(), generatedFilePaths);

            employee.setIncrementStatus("EMAIL_SENT");
            userRepository.save(employee);

        } catch (Exception e) {
            throw new RuntimeException("An error occurred while sending the notification: " + e.getMessage(), e);
        }
    }

    private void generateAutoFilledDocxWithLeave(java.io.InputStream is, Path targetPath, User user, double oldLeave, double currentLeave, double incYearSickLeave) {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(is);
             java.io.OutputStream os = Files.newOutputStream(targetPath)) {

            Map<String, String> dataToReplace = new HashMap<>();
            dataToReplace.put("${employeeName}", user.getUsername() != null ? user.getUsername() : "");
            dataToReplace.put("${dateOfBirth}", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "");
            dataToReplace.put("${designation}", user.getDesignation() != null ? user.getDesignation() : "");
            dataToReplace.put("${department}", user.getDepartment() != null ? user.getDepartment() : "");
            dataToReplace.put("${grade}", user.getGrade() != null ? user.getGrade() : "");
            dataToReplace.put("${incrementDate}", user.getIncrementDate() != null ? user.getIncrementDate().toString() : "");

            dataToReplace.put("${oldYearSickUsed}", String.valueOf((int) oldLeave));
            dataToReplace.put("${currentYearSickUsed}", String.valueOf((int) currentLeave));
            dataToReplace.put("${incrementYearSickUsed}", String.valueOf((int) incYearSickLeave));

            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                replaceTextInParagraph(p, dataToReplace);
            }

            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                List<org.apache.poi.xwpf.usermodel.XWPFRun> runs = p.getRuns();
                if (runs != null) {
                    for (org.apache.poi.xwpf.usermodel.XWPFRun r : runs) {
                        String text = r.getText(0);
                        if (text != null) {
                            for (Map.Entry<String, String> entry : dataToReplace.entrySet()) {
                                if (text.contains(entry.getKey())) {
                                    text = text.replace(entry.getKey(), entry.getValue());
                                    r.setText(text, 0);
                                }
                            }
                        }
                    }
                }
            }

            for (org.apache.poi.xwpf.usermodel.XWPFTable tbl : doc.getTables()) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : tbl.getRows()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : cell.getParagraphs()) {
                            for (org.apache.poi.xwpf.usermodel.XWPFRun r : p.getRuns()) {
                                String text = r.getText(0);
                                if (text != null) {
                                    honestyReplace(r, text, dataToReplace);
                                }
                            }
                        }
                    }
                }
            }

            doc.write(os);

        } catch (IOException e) {
            throw new RuntimeException("Failed to write automated fields to Word file: " + e.getMessage());
        }
    }

    private void honestyReplace(org.apache.poi.xwpf.usermodel.XWPFRun r, String text, Map<String, String> dataToReplace) {
        boolean replaced = false;
        for (Map.Entry<String, String> entry : dataToReplace.entrySet()) {
            if (text.contains(entry.getKey())) {
                text = text.replace(entry.getKey(), entry.getValue());
                replaced = true;
            }
        }
        if (replaced) {
            r.setText(text, 0);
        }
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

                    int currentYear = 2026;
                    if (notification.getCreatedAt() != null) {
                        currentYear = notification.getCreatedAt().getYear();
                        if (currentYear < 1900) {
                            currentYear = java.time.LocalDate.now().getYear();
                        }
                    } else {
                        currentYear = java.time.LocalDate.now().getYear();
                    }
                    int oldYear = currentYear - 1;

                    String employeeEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";

                    double currentSickUsed = 0.0;
                    List<LeaveEntitlement> currentEntitlements = getSickLeaveEntitlements(employeeEmail, currentYear);
                    if (currentEntitlements != null && !currentEntitlements.isEmpty()) {
                        currentSickUsed = currentEntitlements.get(0).getUsedDays();
                    }

                    double oldSickUsed = 0.0;
                    List<LeaveEntitlement> oldEntitlements = getSickLeaveEntitlements(employeeEmail, oldYear);
                    if (oldEntitlements != null && !oldEntitlements.isEmpty()) {
                        oldSickUsed = oldEntitlements.get(0).getUsedDays();
                    }

                    double incYearSickUsed = calculateSickLeaveForIncrementYear(employeeEmail, user.getIncrementDate());

                    return IncrementNotificationResponse.builder()
                            .notificationId(notification.getId())
                            .userId(user.getId())
                            .employeeName(user.getUsername())
                            .email(user.getEmail())
                            .phoneNumber(user.getPhoneNumber())
                            .incrementDate(user.getIncrementDate())
                            .message(notification.getMessage())
                            .status(notification.getStatus())
                            .sentDate(notification.getCreatedAt())
                            .submittedDate(notification.getSubmittedAt())
                            .requestedTemplates(notification.getRequestedTemplates())
                            .generatedFileUrls(notification.getGeneratedFileUrls())
                            .submittedFileUrls(notification.getSubmittedFileUrls())

                            .oldYearSickUsed(oldSickUsed)
                            .currentYearSickUsed(currentSickUsed)

                            .incrementYearSickUsed(incYearSickUsed)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(IncrementNotificationResponse::getSentDate).reversed())
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
        compareAndAdd(fieldChanges, "emergencyContact", userToUpdate.getEmergencyContact(), updateData.getEmergencyContact());
        compareAndAdd(fieldChanges, "gender", userToUpdate.getGender(), updateData.getGender());
        compareAndAdd(fieldChanges, "dateOfBirth",
                userToUpdate.getDateOfBirth() != null ? userToUpdate.getDateOfBirth().toString() : null,
                updateData.getDateOfBirth() != null ? updateData.getDateOfBirth().toString() : null);
        compareAndAdd(fieldChanges, "wnopNumber", userToUpdate.getWnopNumber(), updateData.getWnopNumber());

        if (isAdmin) {
            compareAndAdd(fieldChanges, "serviceNumber", userToUpdate.getServiceNumber(), updateData.getServiceNumber());
            compareAndAdd(fieldChanges, "department", userToUpdate.getDepartment(), updateData.getDepartment());
            compareAndAdd(fieldChanges, "designation", userToUpdate.getDesignation(), updateData.getDesignation());
            compareAndAdd(fieldChanges, "dutyPlace", userToUpdate.getDutyPlace(), updateData.getDutyPlace());
            compareAndAdd(fieldChanges, "grade", userToUpdate.getGrade(), updateData.getGrade());
            compareAndAdd(fieldChanges, "salaryScale", userToUpdate.getSalaryScale(), updateData.getSalaryScale());
            compareAndAdd(fieldChanges, "salary", userToUpdate.getSalary(), updateData.getSalary());

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
                if ("GLOBAL".equalsIgnoreCase(c.getScope())) {
                    configMap.put(c.getFieldKey(), c);
                }
                else if ("DESIGNATION".equalsIgnoreCase(c.getScope())
                        && userToUpdate.getDesignation() != null
                        && c.getTargetDesignations() != null
                        && c.getTargetDesignations().stream().anyMatch(d -> d.trim().equalsIgnoreCase(userToUpdate.getDesignation().trim()))) {
                    configMap.put(c.getFieldKey(), c);
                }
                else if ("SPECIFIC".equalsIgnoreCase(c.getScope())) {
                    configMap.put(c.getFieldKey(), c);
                }
            }

            for (Map.Entry<String, Object> entry : updateData.getDynamicFields().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                DynamicField config = configMap.get(key);
                if (config == null) {
                    continue;
                }

                String oldVal = userToUpdate.getDynamicFields().get(key) != null ? userToUpdate.getDynamicFields().get(key).toString() : "";
                String newVal = value != null ? value.toString() : "";

                if (!oldVal.trim().equals(newVal.trim())) {
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
        userToUpdate.setEmergencyContact(updateData.getEmergencyContact());
        userToUpdate.setProfileImage(updateData.getProfileImage());
        userToUpdate.setGender(updateData.getGender());
        userToUpdate.setDateOfBirth(updateData.getDateOfBirth());
        userToUpdate.setWnopNumber(updateData.getWnopNumber());

        if (isAdmin) {
            userToUpdate.setServiceNumber(updateData.getServiceNumber());
            userToUpdate.setDepartment(updateData.getDepartment());
            userToUpdate.setDesignation(updateData.getDesignation());
            userToUpdate.setDutyPlace(updateData.getDutyPlace());
            userToUpdate.setGrade(updateData.getGrade());
            userToUpdate.setDateOfLanguageProficiency(updateData.getDateOfLanguageProficiency());
            userToUpdate.setSalaryScale(updateData.getSalaryScale());
            userToUpdate.setSalary(updateData.getSalary());
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

    @Transactional
    public DynamicField createDynamicField(DynamicField field) {
        DynamicField savedField = dynamicFieldRepository.save(field);

        if ("DESIGNATION".equalsIgnoreCase(field.getScope()) && field.getTargetDesignations() != null) {
            List<User> employees = userRepository.findByRolesContaining(Role.EMPLOYEE);

            for (User user : employees) {
                if (user.getDesignation() != null) {
                    boolean matchesDesignation = field.getTargetDesignations().stream()
                            .anyMatch(d -> d.trim().equalsIgnoreCase(user.getDesignation().trim()));

                    if (matchesDesignation) {
                        if (user.getDynamicFields() == null) {
                            user.setDynamicFields(new HashMap<>());
                        }
                        if (!user.getDynamicFields().containsKey(field.getFieldKey())) {
                            user.getDynamicFields().put(field.getFieldKey(), "");
                            userRepository.save(user);
                        }
                    }
                }
            }
        }
        return savedField;
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
        return userRepository.findByRolesContaining(Role.EMPLOYEE)
                .stream()
                .filter(User::isActive)
                .toList();
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

    @Override
    @Transactional
    public String generatePodu232Form(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found."));

        User user = userRepository.findById(notification.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for this notification."));

        int currentYear = 2026;
        if (notification.getCreatedAt() != null) {
            currentYear = notification.getCreatedAt().getYear();
            if (currentYear < 1900) {
                currentYear = java.time.LocalDate.now().getYear();
            }
        } else {
            currentYear = java.time.LocalDate.now().getYear();
        }
        int oldYear = currentYear - 1;

        String employeeEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";

        double currentSickUsed = 0.0;
        List<LeaveEntitlement> currentEntitlements = getSickLeaveEntitlements(employeeEmail, currentYear);
        if (currentEntitlements != null && !currentEntitlements.isEmpty()) {
            currentSickUsed = currentEntitlements.get(0).getUsedDays();
        }

        double oldSickUsed = 0.0;
        List<LeaveEntitlement> oldEntitlements = getSickLeaveEntitlements(employeeEmail, oldYear);
        if (oldEntitlements != null && !oldEntitlements.isEmpty()) {
            oldSickUsed = oldEntitlements.get(0).getUsedDays();
        }

        org.springframework.core.io.ClassPathResource resource =
                new org.springframework.core.io.ClassPathResource("templates/increment/පොදු 232 ආකෘතිය.docx");

        if (!resource.exists()) {
            throw new RuntimeException("Template file 'පොදු 232 ආකෘතිය.docx' not found inside resources path.");
        }

        String generatedFileName = System.currentTimeMillis() + "_" + user.getUsername() + "පොදු 232 ආකෘතිය.docx";
        Path uploadPath = Paths.get(UPLOAD_DIR);
        Path targetPath = uploadPath.resolve(generatedFileName);

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            double incYearSickUsed = calculateSickLeaveForIncrementYear(employeeEmail, user.getIncrementDate());

            try (java.io.InputStream is = resource.getInputStream()) {
                generateAutoFilledDocxWithLeave(is, targetPath, user, oldSickUsed, currentSickUsed, incYearSickUsed);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error generating Podu 232 Form: " + e.getMessage());
        }

        return "/" + UPLOAD_DIR + generatedFileName;
    }

    private void generateAutoFilledDocxWithLeave(java.io.InputStream is, Path targetPath, User user, double oldLeave, double currentLeave) {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(is);
             java.io.OutputStream os = Files.newOutputStream(targetPath)) {

            Map<String, String> dataToReplace = new HashMap<>();
            dataToReplace.put("${employeeName}", user.getUsername() != null ? user.getUsername() : "");
            dataToReplace.put("${dateOfBirth}", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "");
            dataToReplace.put("${designation}", user.getDesignation() != null ? user.getDesignation() : "");
            dataToReplace.put("${department}", user.getDepartment() != null ? user.getDepartment() : "");
            dataToReplace.put("${grade}", user.getGrade() != null ? user.getGrade() : "");
            dataToReplace.put("${incrementDate}", user.getIncrementDate() != null ? user.getIncrementDate().toString() : "");

            dataToReplace.put("${oldYearSickUsed}", String.valueOf((int) oldLeave));
            dataToReplace.put("${currentYearSickUsed}", String.valueOf((int) currentLeave));

            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                replaceTextInParagraph(p, dataToReplace);
            }

            for (org.apache.poi.xwpf.usermodel.XWPFTable tbl : doc.getTables()) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : tbl.getRows()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : cell.getParagraphs()) {
                            replaceTextInParagraph(p, dataToReplace);
                        }
                    }
                }
            }

            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                for (org.apache.xmlbeans.XmlObject xObj : p.getCTP().selectPath(
                        "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' " +
                                "declare namespace wps='http://schemas.microsoft.com/office/word/2010/wordprocessingShape' " +
                                ".//wps:txbx//w:p")) {
                    try {
                        org.apache.poi.xwpf.usermodel.XWPFParagraph shapeParagraph = new org.apache.poi.xwpf.usermodel.XWPFParagraph(
                                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP.Factory.parse(xObj.xmlText()), p.getBody());
                        replaceTextInParagraph(shapeParagraph, dataToReplace);
                        xObj.set(shapeParagraph.getCTP());
                    } catch (Exception e) {
                        System.err.println("Error parsing shape text box: " + e.getMessage());
                    }
                }
            }

            doc.write(os);

        } catch (IOException e) {
            throw new RuntimeException("Failed to write automated fields to Word file: " + e.getMessage());
        }
    }

    private void replaceTextInParagraph(org.apache.poi.xwpf.usermodel.XWPFParagraph p, Map<String, String> dataToReplace) {
        String paragraphText = p.getParagraphText();
        if (paragraphText == null || paragraphText.isEmpty()) return;

        boolean needReplacement = false;
        for (String key : dataToReplace.keySet()) {
            if (paragraphText.contains(key)) {
                needReplacement = true;
                break;
            }
        }

        if (needReplacement && p.getRuns() != null && !p.getRuns().isEmpty()) {
            StringBuilder fullTextBuilder = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFRun r : p.getRuns()) {
                String text = r.getText(0);
                if (text != null) fullTextBuilder.append(text);
            }

            String combinedText = fullTextBuilder.toString();
            for (Map.Entry<String, String> entry : dataToReplace.entrySet()) {
                if (combinedText.contains(entry.getKey())) {
                    combinedText = combinedText.replace(entry.getKey(), entry.getValue());
                }
            }

            int runSize = p.getRuns().size();
            for (int i = runSize - 1; i > 0; i--) {
                p.removeRun(i);
            }
            if (!p.getRuns().isEmpty()) {
                p.getRuns().get(0).setText(combinedText, 0);
            }
        }
    }

    @Override
    public double calculateSickLeaveForIncrementYear(String email, java.time.LocalDate incrementDate) {
        if (incrementDate == null || email == null || email.isEmpty()) {
            return 0.0;
        }

        java.time.LocalDate endDate = incrementDate;
        java.time.LocalDate startDate = incrementDate.minusYears(1);

        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        String sanitizedEmail = email.trim().toLowerCase();
        Query query = new Query();
        query.addCriteria(Criteria.where("employeeEmail").is(sanitizedEmail).and("leaveType").is("SICK"));

        List<Document> rawDocuments = leaveTemplate.find(query, Document.class, "Leave Entitlements");

        double totalSickDaysInIncrementYear = 0.0;
        java.time.format.DateTimeFormatter simpleFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Document doc : rawDocuments) {
            Object yearObj = doc.get("year");
            int docYear = 0;
            if (yearObj instanceof Number) {
                docYear = ((Number) yearObj).intValue();
            } else if (yearObj instanceof String) {
                docYear = Integer.parseInt((String) yearObj);
            }

            if (docYear != startYear && docYear != endYear) {
                continue;
            }

            Document monthlyUsage = (Document) doc.get("monthlyUsage");
            if (monthlyUsage == null) continue;

            for (String monthKey : monthlyUsage.keySet()) {
                Document monthDoc = (Document) monthlyUsage.get(monthKey);
                if (monthDoc == null) continue;

                Document sickDoc = (Document) monthDoc.get("SICK");
                if (sickDoc == null) continue;

                List<?> datesList = (List<?>) sickDoc.get("dates");
                if (datesList == null) continue;

                for (Object dateObj : datesList) {
                    String dateStr = (dateObj != null) ? dateObj.toString().trim() : "";
                    if (dateStr.isEmpty()) continue;

                    try {
                        if (dateStr.contains("~")) {
                            String[] parts = dateStr.split("~");
                            String firstDateStr = parts[0].trim();

                            String secondPart = parts[1].trim();
                            String[] subParts = secondPart.split("\\(");
                            String secondDateStr = subParts[0].trim();

                            double days = 0.0;
                            if (subParts.length > 1) {
                                String daysStr = subParts[1].replace("d)", "").trim();
                                days = Double.parseDouble(daysStr);
                            }

                            java.time.LocalDate leaveStart = java.time.LocalDate.parse(firstDateStr, simpleFormatter);
                            java.time.LocalDate leaveEnd = java.time.LocalDate.parse(secondDateStr, simpleFormatter);

                            if ((leaveStart.isAfter(startDate) || leaveStart.isEqual(startDate)) &&
                                    (leaveEnd.isBefore(endDate) || leaveEnd.isEqual(endDate))) {
                                totalSickDaysInIncrementYear += days;
                            }

                        } else if (dateStr.contains("(")) {
                            String[] parts = dateStr.split("\\(");
                            String exactDateStr = parts[0].trim();

                            double days = 0.0;
                            if (parts.length > 1) {
                                String daysStr = parts[1].replace("d)", "").trim();
                                days = Double.parseDouble(daysStr);
                            }

                            java.time.LocalDate leaveDate = java.time.LocalDate.parse(exactDateStr, simpleFormatter);

                            if ((leaveDate.isAfter(startDate) || leaveDate.isEqual(startDate)) &&
                                    (leaveDate.isBefore(endDate) || leaveDate.isEqual(endDate))) {
                                totalSickDaysInIncrementYear += days;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing leave date string: " + dateStr + " - " + e.getMessage());
                    }
                }
            }
        }

        return totalSickDaysInIncrementYear;
    }


    @Override
    @Transactional
    public void saveOrUpdateExcelEmployees(List<User> excelUsers, String adminEmail) {
        for (User excelUser : excelUsers) {
            if (excelUser.getNic() == null || excelUser.getNic().isEmpty()) {
                continue;
            }

            Optional<User> existingUserOpt = userRepository.findByNic(excelUser.getNic());

            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();

                excelUser.setPassword(existingUser.getPassword());
                excelUser.setRoles(existingUser.getRoles());

                var authorities = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PERSONALFILE_ADMIN"));

                this.updatePersonalFile(existingUser.getId(), excelUser, "EXCEL_BULK_UPLOAD (" + adminEmail + ")", authorities);

            } else {
                userRepository.save(excelUser);
            }
        }
    }

    @Override
    public List<User> getEmployeesSortedByLatestHistory() {
        List<DataChangeHistory> allHistory = historyRepository.findAll();

        allHistory.sort((a, b) -> b.getChangedAt().compareTo(a.getChangedAt()));

        java.util.Set<String> uniqueUserIds = new java.util.LinkedHashSet<>();
        for (DataChangeHistory history : allHistory) {
            if (history.getUserId() != null) {
                uniqueUserIds.add(history.getUserId());
            }
        }
        List<User> sortedEmployees = new java.util.ArrayList<>();
        for (String userId : uniqueUserIds) {
            userRepository.findById(userId).ifPresent(sortedEmployees::add);
        }

        return sortedEmployees;
    }

    @Override
    @Transactional
    public void deactivateEmployee(
            String userId,
            String reason,
            java.time.LocalDate deactivatedDate,
            java.time.LocalDate deathDate) {

        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Error: A reason must be provided to deactivate an employee.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (!user.isActive()) {
            throw new RuntimeException("Employee is already deactivated.");
        }

        String trimmedReason = reason.trim();
        boolean isDeathReason = "death".equalsIgnoreCase(trimmedReason);

        if (isDeathReason) {
            if (deathDate == null) {
                throw new RuntimeException("Error: Death date must be provided when the reason is 'Death'.");
            }
            user.setDeathDate(deathDate);
            user.setDeactivatedDate(null);
        } else {
            if (deactivatedDate == null) {
                throw new RuntimeException("Error: Deactivation date must be provided for reason: " + trimmedReason);
            }
            user.setDeathDate(null);
            user.setDeactivatedDate(deactivatedDate);
        }
        user.setActive(false);
        user.setReason(trimmedReason);

        userRepository.save(user);
    }

    @Override
    public List<User> getDeactivatedEmployees() {
        return userRepository.findByRolesContaining(Role.EMPLOYEE)
                .stream()
                .filter(user -> !user.isActive())
                .toList();
    }

    @Override
    @Transactional
    public void activateEmployee(String userId, String reason, java.time.LocalDate activatedDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        boolean isDeactivatedDueToDeath = user.getReason() != null && "death".equalsIgnoreCase(user.getReason().trim());

        if (isDeactivatedDueToDeath || user.getDeathDate() != null) {
            throw new RuntimeException("Error: Cannot activate an employee account marked as Deceased/Death.");
        }

        user.setActive(true);
        user.setReason(reason);

        user.setActivatedDate(activatedDate != null ? activatedDate : java.time.LocalDate.now());

        user.setDeactivatedDate(null);
        user.setDeathDate(null);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void saveDesignationTemplates(String designation, List<String> templateNames) {
        String sanitizedDesignation = designation.trim();
        Optional<DesignationTemplateMapping> existingMapping = designationTemplateMappingRepository.findByDesignation(sanitizedDesignation);

        DesignationTemplateMapping mapping = existingMapping.orElse(new DesignationTemplateMapping());
        mapping.setDesignation(sanitizedDesignation);
        mapping.setTemplateNames(templateNames);

        designationTemplateMappingRepository.save(mapping);
    }

    @Override
    public List<String> getTemplatesForDesignation(String designation) {
        return designationTemplateMappingRepository.findByDesignation(designation.trim())
                .map(DesignationTemplateMapping::getTemplateNames)
                .orElse(Collections.emptyList());
    }

    @Override
    @Transactional
    public void processAutomatedIncrementCheck() {
        java.time.LocalDate targetDate = java.time.LocalDate.now().plusDays(14);
        int targetMonth = targetDate.getMonthValue();
        int targetDay = targetDate.getDayOfMonth();

        List<User> activeEmployees = userRepository.findByRolesContaining(Role.EMPLOYEE)
                .stream()
                .filter(User::isActive)
                .toList();

        for (User employee : activeEmployees) {
            if (employee.getIncrementDate() == null || employee.getDesignation() == null) {
                continue;
            }

            int empMonth = employee.getIncrementDate().getMonthValue();
            int empDay = employee.getIncrementDate().getDayOfMonth();

            if (empMonth == targetMonth && empDay == targetDay) {
                List<String> templates = getTemplatesForDesignation(employee.getDesignation());

                if (!templates.isEmpty()) {
                    try {
                        sendIncrementNotification(employee.getId(), templates);
                        System.out.println("✔ Automated process completed for: " + employee.getUsername());
                    } catch (Exception e) {
                        System.err.println("❌ Error in automated process for: " + employee.getUsername() + " -> " + e.getMessage());
                    }
                }
            }
        }
    }
}