package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO;

import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class DriverDTO {
    private String name;
    private String phoneNumber;
    private String licenseNumber;
    private String nic;
    private String address;
    private String email;
    private String emergencyContact;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate licenseExpiryDate;
}