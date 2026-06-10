package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Role;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByRolesContaining(Role role);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndAddressAndNicAndEmailAndPhoneNumberAndDateOfBirth(
            String username,
            String address,
            String nic,
            String email,
            String phoneNumber,
            LocalDate dateOfBirth
    );
}