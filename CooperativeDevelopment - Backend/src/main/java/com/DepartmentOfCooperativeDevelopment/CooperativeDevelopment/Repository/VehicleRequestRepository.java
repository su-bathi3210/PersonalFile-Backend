package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.RequestStatus;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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
    List<VehicleRequest> findByStatusIn(List<RequestStatus> statuses);

    @Query("{ 'assignedVehicleId': ?0, " +
            "  '_id': { '$ne': ?2 }, " +
            "  'status': { '$nin': ['REJECTED_BY_VEHICLE_ADMIN', 'REJECTED_BY_VEHICLE_APPROVAL_OFFICER', 'EMPLOYEE_CANCELLED', 'COMPLETED'] }, " +
            "  'travelDateTime': ?1 }")
    List<VehicleRequest> findConflictingVehicleRequests(String vehicleId, Date travelDateTime, String currentRequestId);

    @Query("{ 'assignedDriverId': ?0, " +
            "  '_id': { '$ne': ?2 }, " +
            "  'status': { '$nin': ['REJECTED_BY_VEHICLE_ADMIN', 'REJECTED_BY_VEHICLE_APPROVAL_OFFICER', 'EMPLOYEE_CANCELLED', 'COMPLETED'] }, " +
            "  'travelDateTime': ?1 }")
    List<VehicleRequest> findConflictingDriverRequests(String driverId, Date travelDateTime, String currentRequestId);

    @Query("{ 'assignedVehicleId': ?0, " +
            "  '_id': { '$ne': ?3 }, " +
            "  'status': { '$nin': ['REJECTED_BY_VEHICLE_ADMIN', 'REJECTED_BY_VEHICLE_APPROVAL_OFFICER', 'EMPLOYEE_CANCELLED', 'COMPLETED'] }, " +
            "  'travelDateTime': { '$gte': ?1, '$lte': ?2 } }")
    List<VehicleRequest> findConflictingVehicleRequestsInRange(String vehicleId, Date start, Date end, String currentRequestId);

    @Query("{ 'assignedDriverId': ?0, " +
            "  '_id': { '$ne': ?3 }, " +
            "  'status': { '$nin': ['REJECTED_BY_VEHICLE_ADMIN', 'REJECTED_BY_VEHICLE_APPROVAL_OFFICER', 'EMPLOYEE_CANCELLED', 'COMPLETED'] }, " +
            "  'travelDateTime': { '$gte': ?1, '$lte': ?2 } }")
    List<VehicleRequest> findConflictingDriverRequestsInRange(String driverId, Date start, Date end, String currentRequestId);
}