package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Config;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Role;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        if (!userRepository.existsByUsername("vehicle.admin")) {
            User vAdmin = User.builder()
                    .username("vehicle.admin")
                    .email("vadmin@gov.lk")
                    .password(passwordEncoder.encode("Admin@Vehicle#2026"))
                    .roles(Set.of(Role.VEHICLE_ADMIN))
                    .build();
            userRepository.save(vAdmin);
        }

        if (!userRepository.existsByUsername("pf.admin")) {
            User pfAdmin = User.builder()
                    .username("pf.admin")
                    .email("pfadmin@gov.lk")
                    .password(passwordEncoder.encode("Admin@PF#2026"))
                    .roles(Set.of(Role.PERSONALFILE_ADMIN))
                    .build();
            userRepository.save(pfAdmin);
        }

        if (!userRepository.existsByUsername("vehicle.approval")) {
            User vApprove = User.builder()
                    .username("vehicle.approval")
                    .email("vapproval@gov.lk")
                    .password(passwordEncoder.encode("Approve@123"))
                    .roles(Set.of(Role.VEHICLE_APPROVAL))
                    .build();
            userRepository.save(vApprove);
        }

        if (!userRepository.existsByUsername("pf.approval")) {
            User pfApprove = User.builder()
                    .username("pf.approval")
                    .email("pfapproval@gov.lk")
                    .password(passwordEncoder.encode("Approve@123"))
                    .roles(Set.of(Role.PERSONALFILE_APPROVAL))
                    .build();
            userRepository.save(pfApprove);
        }
    }
}