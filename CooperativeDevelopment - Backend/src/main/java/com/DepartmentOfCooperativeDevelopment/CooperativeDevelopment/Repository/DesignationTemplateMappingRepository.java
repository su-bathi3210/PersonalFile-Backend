package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.DesignationTemplateMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface DesignationTemplateMappingRepository extends MongoRepository<DesignationTemplateMapping, String> {
    Optional<DesignationTemplateMapping> findByDesignation(String designation);
}