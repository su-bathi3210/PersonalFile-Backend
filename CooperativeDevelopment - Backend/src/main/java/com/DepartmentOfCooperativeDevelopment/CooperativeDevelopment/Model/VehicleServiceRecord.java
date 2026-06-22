package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "VehicleServiceRecord")
public class VehicleServiceRecord {
    @Id
    private String id;
    private String vehicleId;
    private String vehicleNumber;
    private String driverId;
    private String driverName;

    private Double serviceKm;
    private Double serviceCost;
    private Double nextServiceKm;
    private String description;

    private LocalDateTime servicedAt;
}