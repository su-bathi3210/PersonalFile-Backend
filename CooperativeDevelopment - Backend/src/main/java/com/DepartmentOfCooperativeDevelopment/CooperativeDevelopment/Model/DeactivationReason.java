package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "DeactivationReasons")
public class DeactivationReason {
    @Id
    private String id;
    private String reasonText;
    private String type;
}