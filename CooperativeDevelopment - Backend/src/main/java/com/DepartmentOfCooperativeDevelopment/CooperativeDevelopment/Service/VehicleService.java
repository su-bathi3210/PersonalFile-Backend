package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Vehicle;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleServiceRecord;

import java.util.List;

public interface VehicleService {
    Vehicle addVehicle(VehicleDTO dto);
    List<Vehicle> getAllVehicles();
    Vehicle updateVehicle(String id, VehicleDTO dto);
    void deleteVehicle(String id);
    VehicleServiceRecord addServiceRecord(String vehicleId, Double serviceCost, Double nextServiceKm, String description, String driverNic, Double serviceKm);
}