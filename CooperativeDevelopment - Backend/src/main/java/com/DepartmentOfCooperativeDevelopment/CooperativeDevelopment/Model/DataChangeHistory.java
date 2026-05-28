package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "DataChangeHistory")
public class DataChangeHistory {

    @Id
    private String id;
    private String userId;
    private String employeeName;
    private String changedBy;
    private LocalDateTime changedAt;
    private int revisionNumber;

    private List<FieldChange> changes;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldChange {
        private String fieldName;
        private String oldValue;
        private String newValue;    
    }
}