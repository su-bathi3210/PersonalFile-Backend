package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Vehicle;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleServiceRecord;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.VehicleServiceRecordRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    private final VehicleServiceRecordRepository serviceRecordRepository;

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
    @PreAuthorize("hasAnyRole('VEHICLE_ADMIN', 'EMPLOYEE', 'VEHICLE_APPROVAL', 'DRIVER')")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<?> updateVehicle(@PathVariable String id, @RequestBody VehicleDTO vehicleDTO) {
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicleDTO);
            return ResponseEntity.ok(updatedVehicle);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<?> deleteVehicle(@PathVariable String id) {
        try {
            vehicleService.deleteVehicle(id);
            return ResponseEntity.ok("Vehicle deleted successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{vehicleId}/service-record")
    @PreAuthorize("hasAnyRole('DRIVER', 'VEHICLE_ADMIN')")
    public ResponseEntity<?> createServiceRecord(
            @PathVariable String vehicleId,
            @RequestParam Double serviceCost,
            @RequestParam Double nextServiceKm,
            @RequestParam Double serviceKm,
            @RequestParam String description,
            Principal principal) {
        try {
            String loggedInUserNic = principal.getName();

            VehicleServiceRecord saved = vehicleService.addServiceRecord(
                    vehicleId,
                    serviceCost,
                    nextServiceKm,
                    description,
                    loggedInUserNic,
                    serviceKm
            );
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/service-history")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<List<VehicleServiceRecord>> getServiceLogHistory() {
        return ResponseEntity.ok(serviceRecordRepository.findAllByOrderByServicedAtDesc());
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<Long> getVehiclesCount() {
        long count = vehicleService.getAllVehicles().size();
        return ResponseEntity.ok(count);
    }
}