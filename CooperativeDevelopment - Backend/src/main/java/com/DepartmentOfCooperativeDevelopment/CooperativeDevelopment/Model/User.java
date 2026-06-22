package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Transient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Users")
public class User {

    @Id
    private String id;
    private String profileImage;
    private String username;
    private String password;
    private Set<Role> roles;

    private String email;

    @Indexed(unique = true)
    private String nic;
    
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
    private String phoneNumber;

    private String emergencyContact;
    private String salary;

    private String serviceNumber;
    private LocalDate dateOfLanguageProficiency;

    private String wnopNumber;
    private String department;
    private String designation;
    private LocalDate dateOfFirstAppointment;
    private LocalDate appointmentDateToPresentStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate incrementDate;

    private String dutyPlace;
    private String grade;
    private LocalDate dateOfReceiptOfRelevantGrade;
    private String salaryScale;
    private LocalDate dateOfCompulsoryRetirement;

    private LocalDate presentStatusDate;
    private LocalDate dateOfReceiptGradeI;
    private LocalDate dateOfReceiptGradeII;
    private LocalDate dateOfReceiptGradeIII;

    private String incrementStatus;
    private Map<String, Object> dynamicFields = new HashMap<>();
}