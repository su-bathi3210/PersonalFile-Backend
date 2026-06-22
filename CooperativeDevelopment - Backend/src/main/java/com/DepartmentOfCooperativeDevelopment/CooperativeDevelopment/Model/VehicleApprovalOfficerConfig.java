package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "VehicleApprovalOfficerConfigs")
public class VehicleApprovalOfficerConfig {
    @Id
    private String id = "VEHICLE_APPROVAL_OFFICER_SETTINGS";
    private String officerEmail;
}