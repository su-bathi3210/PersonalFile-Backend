package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleApprovalDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleRequestDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleRequestUpdateDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.RequestStatus;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleRequest;
import java.util.List;

public interface VehicleRequestService {
    VehicleRequest createRequest(VehicleRequestDTO dto);
    List<VehicleRequest> getAllPendingRequests();
    List<VehicleRequest> getRequestsByEmployee(String requesterEmail);
    VehicleRequest updateRequestStatus(String requestId, RequestStatus status);
    VehicleRequest approveByApprovalOfficer(String requestId, String officerRemarks);
    List<VehicleRequest> getRequestsByStatus(RequestStatus status);
    VehicleRequest rejectByAdmin(String requestId, String adminRemarks);
    VehicleRequest rejectByOfficer(String requestId, String officerRemarks);
    List<VehicleRequest> getOfficerApprovedRequests();
    VehicleRequest completeVehicleRequest(String requestId);
    List<VehicleRequest> getAllRequestsForAdmin();
    List<VehicleRequest> getDriverDashboardTrips(String driverPhoneNumber);
    List<VehicleRequest> getDriverDashboardTripsByNic(String nic);
    VehicleRequest endVehicleTrip(String requestId);
    VehicleRequest cancelRequestByEmployee(String requestId, String employeeEmail);
    void updateVehicleAdminEmail(String email);
    String getVehicleAdminEmail();
    void updateVehicleApprovalOfficerEmail(String email);
    String getVehicleApprovalOfficerEmail();
    long getPendingRequestsCountForAdmin();
    long getOfficerApprovedRequestsCountForAdmin();
    List<VehicleRequest> getTodayVehicleRequests();
    void sendTodayTripsToAdmin();
    VehicleRequest startTrip(String id);
    VehicleRequest updateVehicleRequestByEmployee(String requestId, String employeeEmail, VehicleRequestUpdateDTO dto);
    VehicleRequest approveVehicleRequest(String requestId, VehicleApprovalDTO approvalDTO);
}