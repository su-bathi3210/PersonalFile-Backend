package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VehicleDTO {
    private String vehicleNumber;
    private String vehicleType;
    private String manufacturer;
    private String model;
    private String licenseNumber;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpiryDate;
}