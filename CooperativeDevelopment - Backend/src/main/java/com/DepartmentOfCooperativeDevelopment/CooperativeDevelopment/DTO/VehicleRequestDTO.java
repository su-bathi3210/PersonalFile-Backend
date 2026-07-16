package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    private Date travelDateTime;
    private String reason;
}