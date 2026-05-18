package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String username;
    private List<String> roles;
}