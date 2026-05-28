package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

public enum RequestStatus {
    PENDING,
    APPROVED_BY_VEHICLE_ADMIN,
    APPROVED_BY_VEHICLE_APPROVAL_OFFICER,
    REJECTED_BY_VEHICLE_ADMIN,
    REJECTED_BY_VEHICLE_APPROVAL_OFFICER,
    REJECTED,
    COMPLETED,
    TRIP_STARTED
}