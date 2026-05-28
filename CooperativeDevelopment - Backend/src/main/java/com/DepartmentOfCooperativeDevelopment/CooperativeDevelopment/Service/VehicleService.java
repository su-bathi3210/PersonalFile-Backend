package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Vehicle;
import java.util.List;

public interface VehicleService {
    Vehicle addVehicle(VehicleDTO dto);
    List<Vehicle> getAllVehicles();
}