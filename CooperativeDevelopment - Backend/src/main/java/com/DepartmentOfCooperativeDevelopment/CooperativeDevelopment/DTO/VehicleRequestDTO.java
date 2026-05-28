package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO;

import lombok.Data;
import java.util.Date;

@Data
public class VehicleRequestDTO {
    private String requesterEmail;
    private String requesterName;
    private String requesterPosition;
    private String travelerName;
    private String travelerPosition;
    private String department;
    private String phoneNumber;
    private String dutyNature;
    private String fromLocation;
    private String toLocation;
    private Double distanceKm;
    private Date travelDateTime;
    private String reason;
}