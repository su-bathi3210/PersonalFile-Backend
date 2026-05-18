package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO;

import lombok.Data;

@Data
public class ForgotRequest {
    private String email;
    private String serviceNumber;
}