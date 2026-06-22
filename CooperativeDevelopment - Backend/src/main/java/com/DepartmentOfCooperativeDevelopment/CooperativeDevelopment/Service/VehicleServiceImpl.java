package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    private final DriverRepository driverRepository;

    private final EmailService emailService;

    private final VehicleAdminConfigRepository adminConfigRepository;

    @Autowired
    private VehicleServiceRecordRepository serviceRecordRepository;

    @Override
    public Vehicle addVehicle(VehicleDTO dto) {
        if (vehicleRepository.existsByVehicleNumber(dto.getVehicleNumber())) {
            throw new RuntimeException("Error: A vehicle with this Vehicle Number already exists!");
        }

        Vehicle vehicle = new Vehicle();
        BeanUtils.copyProperties(dto, vehicle);

        vehicle.setCurrentKm(dto.getCurrentKm() != null ? dto.getCurrentKm() : 0.0);
        vehicle.setStatus("AVAILABLE");

        return vehicleRepository.save(vehicle);
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        for (Vehicle vehicle : vehicles) {
            List<VehicleServiceRecord> fullServiceHistory = serviceRecordRepository
                    .findByVehicleIdOrderByServicedAtDesc(vehicle.getId());
            vehicle.setServiceHistorySummary(fullServiceHistory);
        }
        return vehicles;
    }

    @Override
    public Vehicle updateVehicle(String id, VehicleDTO dto) {
        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Vehicle not found!"));

        if (!existingVehicle.getVehicleNumber().equalsIgnoreCase(dto.getVehicleNumber())
                && vehicleRepository.existsByVehicleNumber(dto.getVehicleNumber())) {
            throw new RuntimeException("Error: A vehicle with this Vehicle Number already exists!");
        }

        BeanUtils.copyProperties(dto, existingVehicle, "id", "status", "currentKm");
        return vehicleRepository.save(existingVehicle);
    }

    @Override
    public void deleteVehicle(String id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Vehicle not found!"));

        if ("BOOKED".equalsIgnoreCase(vehicle.getStatus())) {
            throw new RuntimeException("Error: Cannot delete a vehicle that is currently BOOKED for a trip!");
        }

        vehicleRepository.deleteById(id);
    }

    @Override
    @Transactional
    public VehicleServiceRecord addServiceRecord(String vehicleId, Double serviceCost, Double nextServiceKm, String description, String driverNic, Double serviceKm) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Error: Vehicle not found!"));

        Driver driver = driverRepository.findByNic(driverNic)
                .orElseThrow(() -> new RuntimeException("Error: Driver not found!"));

        if (serviceKm <= 0) {
            throw new RuntimeException("Error: Service KM reading must be greater than 0!");
        }

        if (nextServiceKm <= serviceKm) {
            throw new RuntimeException("Error: Next service KM must be greater than service KM (" + serviceKm + " KM)!");
        }

        VehicleServiceRecord record = VehicleServiceRecord.builder()
                .vehicleId(vehicle.getId())
                .vehicleNumber(vehicle.getVehicleNumber())
                .driverId(driver.getId())
                .driverName(driver.getName())
                .serviceKm(serviceKm)
                .serviceCost(serviceCost)
                .nextServiceKm(nextServiceKm)
                .description(description)
                .servicedAt(LocalDateTime.now())
                .build();

        VehicleServiceRecord savedRecord = serviceRecordRepository.save(record);

        vehicle.setCurrentKm(serviceKm);
        vehicle.setNextServiceDueDateKm(nextServiceKm);

        vehicleRepository.save(vehicle);

        String adminEmail = adminConfigRepository.findById("VEHICLE_ADMIN_SETTINGS")
                .map(VehicleAdminConfig::getAdminEmail).orElse(null);

        if (adminEmail != null && !adminEmail.isEmpty()) {
            try {
                emailService.sendAdminNotificationOnServiceRecord(
                        adminEmail,
                        vehicle.getVehicleNumber(),
                        vehicle.getManufacturer() + " " + vehicle.getModel(),
                        driver.getName(),
                        serviceKm,
                        nextServiceKm,
                        serviceCost,
                        description
                );
            } catch (Exception e) {
                System.out.println("Admin Email sending failed on Service Record: " + e.getMessage());
            }
        }
        return savedRecord;
    }
}