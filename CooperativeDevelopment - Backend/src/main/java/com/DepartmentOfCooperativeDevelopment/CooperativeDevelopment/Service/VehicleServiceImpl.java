package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Vehicle;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    public Vehicle addVehicle(VehicleDTO dto) {
        if (vehicleRepository.existsByVehicleNumber(dto.getVehicleNumber())) {
            throw new RuntimeException("Error: A vehicle with this Vehicle Number already exists!");
        }

        Vehicle vehicle = new Vehicle();
        BeanUtils.copyProperties(dto, vehicle);

        vehicle.setStatus("AVAILABLE");

        return vehicleRepository.save(vehicle);
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }
}