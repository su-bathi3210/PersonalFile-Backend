package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.DriverDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Driver;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Role;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.DriverRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Driver addDriver(DriverDTO dto) {
        if (driverRepository.existsByNic(dto.getNic())) {
            throw new RuntimeException("Error: A driver with this NIC already exists!");
        }

        if (driverRepository.existsByLicenseNumber(dto.getLicenseNumber())) {
            throw new RuntimeException("Error: A driver with this License Number already exists!");
        }

        Driver driver = new Driver();
        BeanUtils.copyProperties(dto, driver);
        driver.setStatus("AVAILABLE");
        Driver savedDriver = driverRepository.save(driver);

        User driverUser = User.builder()
                .username(dto.getNic())
                .email(dto.getNic())
                .password(passwordEncoder.encode(dto.getPhoneNumber()))
                .phoneNumber(dto.getPhoneNumber())
                .nic(dto.getNic())
                .roles(Set.of(Role.DRIVER))
                .build();

        userRepository.save(driverUser);

        return savedDriver;
    }

    @Override
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    @Override
    @Transactional
    public Driver updateDriver(String id, DriverDTO dto) {
        Driver existingDriver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Driver not found!"));

        if (!existingDriver.getNic().equals(dto.getNic()) && driverRepository.existsByNic(dto.getNic())) {
            throw new RuntimeException("Error: A driver with this NIC already exists!");
        }

        String oldNic = existingDriver.getNic();

        BeanUtils.copyProperties(dto, existingDriver, "id", "status");
        Driver updatedDriver = driverRepository.save(existingDriver);

        userRepository.findByNic(oldNic).ifPresent(user -> {
            user.setUsername(dto.getNic());
            user.setEmail(dto.getNic());
            user.setPhoneNumber(dto.getPhoneNumber());
            user.setNic(dto.getNic());
            user.setPassword(passwordEncoder.encode(dto.getPhoneNumber()));
            userRepository.save(user);
        });

        return updatedDriver;
    }

    @Override
    @Transactional
    public void deleteDriver(String id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Driver not found!"));

        if ("BOOKED".equalsIgnoreCase(driver.getStatus())) {
            throw new RuntimeException("Error: Cannot delete a driver who is currently BOOKED for a trip!");
        }

        userRepository.findByNic(driver.getNic()).ifPresent(user -> userRepository.delete(user));

        driverRepository.deleteById(id);
    }
}