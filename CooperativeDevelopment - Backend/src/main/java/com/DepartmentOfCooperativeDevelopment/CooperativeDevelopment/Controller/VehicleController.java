package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Vehicle;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<?> createVehicle(@RequestBody VehicleDTO vehicleDTO) {
        try {
            Vehicle savedVehicle = vehicleService.addVehicle(vehicleDTO);
            return ResponseEntity.ok(savedVehicle);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('VEHICLE_ADMIN', 'EMPLOYEE', 'VEHICLE_APPROVAL')")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }
}