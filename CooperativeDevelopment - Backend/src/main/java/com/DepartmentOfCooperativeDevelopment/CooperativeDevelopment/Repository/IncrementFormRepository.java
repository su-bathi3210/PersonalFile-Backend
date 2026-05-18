package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.IncrementForm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IncrementFormRepository extends MongoRepository<IncrementForm, String> {
    List<IncrementForm> findBySubmittedTrue();
}