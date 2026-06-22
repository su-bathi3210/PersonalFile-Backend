package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleServiceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface VehicleServiceRecordRepository extends MongoRepository<VehicleServiceRecord, String> {
    List<VehicleServiceRecord> findByVehicleIdOrderByServicedAtDesc(String vehicleId);
    List<VehicleServiceRecord> findAllByOrderByServicedAtDesc();
}