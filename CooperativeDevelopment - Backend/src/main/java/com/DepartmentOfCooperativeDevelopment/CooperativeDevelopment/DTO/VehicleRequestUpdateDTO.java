package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO;

import lombok.Data;
import java.util.Date;

@Data
public class VehicleRequestUpdateDTO {
    private String fromLocation;
    private String toLocation;
    private Double distanceKm;
    private Date travelDateTime;
    private String dutyNature;
    private String reason;
    private String phoneNumber;
    private String requesterPosition;
    private String department;
}