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

        // 1. Driver Collection එකට දත්ත ඇතුළත් කිරීම
        Driver driver = new Driver();
        BeanUtils.copyProperties(dto, driver);
        driver.setStatus("AVAILABLE");
        Driver savedDriver = driverRepository.save(driver);

        // 2. Driver ට සිස්ටම් එකට ලොග් වෙන්න User Collection එකේ Account එකක් හැදීම
        User driverUser = User.builder()
                .username(dto.getNic())            // 👈 Login Username eka widihata NIC eka gani
                .email(dto.getNic())               // Driverge email eka email field ekata dāwi
                .password(passwordEncoder.encode(dto.getPhoneNumber())) // 👈 Login Password eka widihata Phone Number eka gani
                .phoneNumber(dto.getPhoneNumber())
                .nic(dto.getNic())
                .roles(Set.of(Role.DRIVER))        // Role Enum ekata ROLE_DRIVER athulath wiya yuthuya
                .build();

        userRepository.save(driverUser);

        return savedDriver;
    }

    @Override
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }
}