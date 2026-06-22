package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "Vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    @Id

    @Transient
    private List<VehicleServiceRecord> serviceHistorySummary;

    private String id;
    private String vehicleNumber;
    private String vehicleType;
    private String manufacturer;
    private String model;
    private String status = "AVAILABLE";
    private String licenseNumber;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpiryDate;
    private Double currentKm = 0.0;
    private Double nextServiceDueDateKm;
}