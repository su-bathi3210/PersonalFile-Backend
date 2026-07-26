package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "VehicleRequests")
public class VehicleRequest {
    @Id
    private String id;
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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Colombo")
    private Date travelDateTime;

    private String reason;

    private String assignedVehicleId;
    private String assignedDriverId;

    @DBRef
    private Vehicle assignedVehicle;
    @DBRef
    private Driver assignedDriver;

    private String officerRemarks;
    private String adminRemarks;

    private RequestStatus status = RequestStatus.PENDING;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Colombo")
    private Date createdAt = new Date();
}