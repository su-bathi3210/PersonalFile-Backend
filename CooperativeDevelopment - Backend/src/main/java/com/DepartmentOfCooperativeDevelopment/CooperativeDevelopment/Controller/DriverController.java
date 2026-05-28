package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.DriverDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Driver;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.DriverService;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.VehicleRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    private final VehicleRequestService vehicleRequestService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<?> createDriver(@RequestBody DriverDTO driverDTO) {
        try {
            Driver savedDriver = driverService.addDriver(driverDTO);
            return ResponseEntity.ok(savedDriver);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('VEHICLE_ADMIN', 'EMPLOYEE', 'VEHICLE_APPROVAL')")
    public ResponseEntity<List<Driver>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<List<VehicleRequest>> getDriverDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        String driverNic = userDetails.getUsername();
        List<VehicleRequest> trips = vehicleRequestService.getDriverDashboardTripsByNic(driverNic);
        return ResponseEntity.ok(trips);
    }
}