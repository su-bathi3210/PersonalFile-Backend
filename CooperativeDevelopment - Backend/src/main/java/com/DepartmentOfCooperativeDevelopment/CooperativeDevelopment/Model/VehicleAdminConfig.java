package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "VehicleAdminConfigs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleAdminConfig {
    @Id
    private String id = "VEHICLE_ADMIN_SETTINGS";
    private String adminEmail;
}