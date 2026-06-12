package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Leave;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Leave Entitlements")
public class LeaveEntitlement {
    @Id
    private String id;
    private String employeeEmail;
    private String leaveType;
    private int totalEntitlement;
    private double usedDays;
    private double remainingDays;
    private int year;
    private double carryOverDays;
}