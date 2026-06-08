package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.DynamicField;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DynamicFieldRepository extends MongoRepository<DynamicField, String> {
    Optional<DynamicField> findByFieldKey(String fieldKey);
    boolean existsByFieldKey(String fieldKey);
    List<DynamicField> findByIsGlobalTrueOrEmployeeEmail(String email);
}