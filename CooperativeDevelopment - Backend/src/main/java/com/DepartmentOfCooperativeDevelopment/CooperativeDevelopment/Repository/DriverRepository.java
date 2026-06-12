package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {
    boolean existsByNic(String nic);
    boolean existsByLicenseNumber(String licenseNumber);
    Optional<Driver> findByNic(String nic);
    Optional<Driver> findByPhoneNumber(String phoneNumber);
}