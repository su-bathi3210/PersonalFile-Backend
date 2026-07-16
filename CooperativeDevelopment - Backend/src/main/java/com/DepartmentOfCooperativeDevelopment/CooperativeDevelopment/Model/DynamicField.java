package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "DynamicFields")
public class DynamicField {
    @Id
    private String id;
    private String fieldKey;
    private String displayName;
    private String fieldType;
    private boolean required;
    private String employeeEmail;

    @JsonProperty("isGlobal")
    private boolean isGlobal;

    @JsonProperty("isAdminOnly")
    private boolean isAdminOnly;

    private String scope;
    private String targetDesignation;
}