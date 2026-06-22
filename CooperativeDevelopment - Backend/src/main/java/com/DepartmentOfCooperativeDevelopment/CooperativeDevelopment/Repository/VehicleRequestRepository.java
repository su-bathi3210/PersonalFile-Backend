package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.RequestStatus;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface VehicleRequestRepository extends MongoRepository<VehicleRequest, String> {
    List<VehicleRequest> findByStatus(RequestStatus status);
    List<VehicleRequest> findByRequesterEmail(String requesterEmail);
    List<VehicleRequest> findByAssignedDriverIdAndStatus(String driverId, RequestStatus status);
    List<VehicleRequest> findByAssignedDriverId(String driverId);
    long countByStatus(RequestStatus status);
    List<VehicleRequest> findByTravelDateTimeBetween(Date start, Date end);
}