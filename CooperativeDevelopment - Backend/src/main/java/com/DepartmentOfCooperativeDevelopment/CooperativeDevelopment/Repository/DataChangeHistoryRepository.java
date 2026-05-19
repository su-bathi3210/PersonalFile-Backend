package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.DataChangeHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DataChangeHistoryRepository extends MongoRepository<DataChangeHistory, String> {
    List<DataChangeHistory> findByUserIdOrderByChangedAtDesc(String userId);
    long countByUserId(String userId);
}