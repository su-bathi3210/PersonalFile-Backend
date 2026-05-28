package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleApprovalDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleRequestDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.RequestStatus;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.VehicleRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicle-requests")
@RequiredArgsConstructor
public class VehicleRequestController {

    private final VehicleRequestService vehicleRequestService;

    @PostMapping
    public ResponseEntity<VehicleRequest> createVehicleRequest(@RequestBody VehicleRequestDTO dto) {
        VehicleRequest createdRequest = vehicleRequestService.createRequest(dto);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<VehicleRequest>> getPendingRequests() {
        List<VehicleRequest> pendingRequests = vehicleRequestService.getAllPendingRequests();
        return ResponseEntity.ok(pendingRequests);
    }

    @GetMapping("/employee/{requesterEmail}")
    public ResponseEntity<List<VehicleRequest>> getRequestsByEmployee(@PathVariable String requesterEmail) {
        List<VehicleRequest> employeeRequests = vehicleRequestService.getRequestsByEmployee(requesterEmail);
        return ResponseEntity.ok(employeeRequests);
    }

    @PutMapping("/{requestId}/status")
    public ResponseEntity<VehicleRequest> updateRequestStatus(
            @PathVariable String requestId,
            @RequestParam RequestStatus status) {

        VehicleRequest updatedRequest = vehicleRequestService.updateRequestStatus(requestId, status);
        return ResponseEntity.ok(updatedRequest);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')") 
    public ResponseEntity<VehicleRequest> approveRequest(
            @PathVariable String id,
            @RequestBody VehicleApprovalDTO approvalDTO) {

        VehicleRequest approvedRequest = vehicleRequestService.approveVehicleRequest(id, approvalDTO);
        return ResponseEntity.ok(approvedRequest);
    }

    @PostMapping("/admin-approve/{requestId}")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<VehicleRequest> approveByAdmin(
            @PathVariable String requestId,
            @RequestBody VehicleApprovalDTO approvalDTO) {
        VehicleRequest approvedRequest = vehicleRequestService.approveVehicleRequest(requestId, approvalDTO);
        return ResponseEntity.ok(approvedRequest);
    }

    @GetMapping("/admin-approved-list")
    @PreAuthorize("hasRole('VEHICLE_APPROVAL') or hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<List<VehicleRequest>> getAdminApprovedRequests() {
        List<VehicleRequest> requests = vehicleRequestService.getRequestsByStatus(RequestStatus.APPROVED_BY_VEHICLE_ADMIN);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/officer-approve/{requestId}")
    @PreAuthorize("hasRole('VEHICLE_APPROVAL')")
    public ResponseEntity<VehicleRequest> approveByOfficer(
            @PathVariable String requestId,
            @RequestParam(required = false) String remarks) {
        VehicleRequest finalizedRequest = vehicleRequestService.approveByApprovalOfficer(requestId, remarks);
        return ResponseEntity.ok(finalizedRequest);
    }

    @PostMapping("/admin-reject/{requestId}")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<VehicleRequest> rejectByAdmin(
            @PathVariable String requestId,
            @RequestParam(required = false) String remarks) {
        VehicleRequest request = vehicleRequestService.rejectByAdmin(requestId, remarks);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/officer-reject/{requestId}")
    @PreAuthorize("hasRole('VEHICLE_APPROVAL')")
    public ResponseEntity<VehicleRequest> rejectByOfficer(
            @PathVariable String requestId,
            @RequestParam(required = false) String remarks) {
        VehicleRequest request = vehicleRequestService.rejectByOfficer(requestId, remarks);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<List<VehicleRequest>> getAllRequests() {
        List<VehicleRequest> allRequests = vehicleRequestService.getRequestsByStatus(null);
        return ResponseEntity.ok(allRequests);
    }

    @GetMapping("/officer-approved-list")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<List<VehicleRequest>> getOfficerApprovedRequests() {
        List<VehicleRequest> requests = vehicleRequestService.getOfficerApprovedRequests();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/complete/{requestId}")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<VehicleRequest> completeRequest(@PathVariable String requestId) {
        VehicleRequest completedRequest = vehicleRequestService.completeVehicleRequest(requestId);
        return ResponseEntity.ok(completedRequest);
    }

    @GetMapping("/admin/all-requests")
    @PreAuthorize("hasAnyRole('VEHICLE_ADMIN', 'EMPLOYEE', 'VEHICLE_APPROVAL')")
    public ResponseEntity<List<VehicleRequest>> getAllRequestsForAdminDashboard() {
        List<VehicleRequest> allRequests = vehicleRequestService.getAllRequestsForAdmin();
        return ResponseEntity.ok(allRequests);
    }

    @PostMapping("/end-trip/{requestId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<VehicleRequest> endTripByDriver(@PathVariable String requestId) {
        try {
            VehicleRequest finalizedRequest = vehicleRequestService.endVehicleTrip(requestId);
            return ResponseEntity.ok(finalizedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}