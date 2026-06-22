package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Drivers")
public class Driver {
    @Id
    private String id;
    private String name;
    private String phoneNumber;
    private String licenseNumber;
    private String nic;
    private String address;
    private String email;
    private String status = "AVAILABLE";
    private String emergencyContact;
    private LocalDate licenseExpiryDate;
}