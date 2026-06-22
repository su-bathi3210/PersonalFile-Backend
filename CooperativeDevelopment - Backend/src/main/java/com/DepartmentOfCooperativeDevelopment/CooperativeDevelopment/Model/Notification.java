package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Notifications")
public class Notification {
    @Id
    private String id;
    private String userId;
    private String message;
    private LocalDateTime createdAt;
    private LocalDate originalIncrementDate;
    private boolean isIncrementType;
    private boolean read;
    private String status;

    private List<String> requestedTemplates;
    private List<String> generatedFileUrls;
    private List<String> submittedFileUrls;
    private LocalDateTime submittedAt;
}