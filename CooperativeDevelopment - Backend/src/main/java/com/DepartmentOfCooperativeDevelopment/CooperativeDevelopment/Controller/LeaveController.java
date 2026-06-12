package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Leave.LeaveEntitlement;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final UserService userService;

    @GetMapping("/employee-sick-leaves")
    @PreAuthorize("hasAnyRole('PERSONALFILE_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> getEmployeeSickLeaveData(
            @RequestParam String email,
            @RequestParam int year) {

        List<LeaveEntitlement> entitlements = userService.getSickLeaveEntitlements(email, year);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("currentYear", year);
        response.put("sickLeaveEntitlements", entitlements);

        return ResponseEntity.ok(response);
    }
}