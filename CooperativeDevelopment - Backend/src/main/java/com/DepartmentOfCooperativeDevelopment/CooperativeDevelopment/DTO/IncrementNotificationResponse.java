package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class IncrementNotificationResponse {
    private String notificationId;
    private String userId;
    private String nextIncrementDate;
    private String employeeName;
    private String email;
    private String phoneNumber;
    private LocalDate incrementDate;
    private String message;
    private String status;
    private LocalDateTime sentDate;
    private LocalDateTime submittedDate;

    private List<String> requestedTemplates;
    private List<String> submittedFileUrls;
}