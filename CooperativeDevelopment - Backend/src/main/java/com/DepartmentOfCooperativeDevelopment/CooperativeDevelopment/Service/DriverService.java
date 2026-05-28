package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.DriverDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Driver;
import java.util.List;

public interface DriverService {
    Driver addDriver(DriverDTO dto);
    List<Driver> getAllDrivers();
}