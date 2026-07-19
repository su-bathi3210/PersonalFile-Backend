package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.DeactivationReason;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DeactivationReasonRepository extends MongoRepository<DeactivationReason, String> {
    List<DeactivationReason> findByType(String type);
}