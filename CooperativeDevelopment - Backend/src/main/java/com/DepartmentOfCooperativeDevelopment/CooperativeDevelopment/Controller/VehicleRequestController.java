package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleApprovalDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleRequestDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleRequestUpdateDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.RequestStatus;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleRequest;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.UserRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.VehicleRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/vehicle-requests")
@RequiredArgsConstructor
public class VehicleRequestController {

    private final VehicleRequestService vehicleRequestService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile/{email}")
    public ResponseEntity<?> getUserProfileByEmail(@PathVariable String email) {
        java.util.Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
            errorResponse.put("message", "User not found with email: " + email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/designations-summary")
    public ResponseEntity<?> getDesignationsSummary() {
        List<User> allUsers = userRepository.findAll();
        Map<String, Long> summary = allUsers.stream()
                .filter(u -> u.getDesignation() != null && !u.getDesignation().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        user -> user.getDesignation().toUpperCase(),
                        Collectors.counting()
                ));
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/by-designation")
    public ResponseEntity<?> getUsersByDesignation(@RequestParam String designation) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getDesignation() != null && u.getDesignation().equalsIgnoreCase(designation))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

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
    @PreAuthorize("hasAnyRole('DRIVER', 'VEHICLE_ADMIN')")
    public ResponseEntity<VehicleRequest> endTripByDriver(@PathVariable String requestId) {
        try {
            VehicleRequest finalizedRequest = vehicleRequestService.endVehicleTrip(requestId);
            return ResponseEntity.ok(finalizedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancelRequestByEmployee(
            @PathVariable String requestId,
            java.security.Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User cannot be identified.");
            }

            String employeeEmail = principal.getName();
            VehicleRequest cancelledRequest = vehicleRequestService.cancelRequestByEmployee(requestId, employeeEmail);

            return ResponseEntity.ok(cancelledRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/admin-email")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<String> updateAdminEmail(@RequestParam String email) {
        vehicleRequestService.updateVehicleAdminEmail(email);
        return ResponseEntity.ok("Vehicle Admin email updated successfully.");
    }

    @GetMapping("/admin-email")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<String> getAdminEmail() {
        String email = vehicleRequestService.getVehicleAdminEmail();
        return ResponseEntity.ok(email);
    }

    @PutMapping("/officer-email")
    @PreAuthorize("hasRole('VEHICLE_APPROVAL')")
    public ResponseEntity<String> updateOfficerEmail(@RequestParam String email) {
        vehicleRequestService.updateVehicleApprovalOfficerEmail(email);
        return ResponseEntity.ok("Vehicle Approval Officer email updated successfully.");
    }

    @GetMapping("/officer-email")
    @PreAuthorize("hasRole('VEHICLE_APPROVAL')")
    public ResponseEntity<String> getOfficerEmail() {
        String email = vehicleRequestService.getVehicleApprovalOfficerEmail();
        return ResponseEntity.ok(email);
    }

    @GetMapping("/admin/pending-count")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<Long> getPendingRequestsCount() {
        long count = vehicleRequestService.getPendingRequestsCountForAdmin();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/admin/officer-approved-count")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<Long> getOfficerApprovedRequestsCount() {
        long count = vehicleRequestService.getOfficerApprovedRequestsCountForAdmin();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/today")
    public ResponseEntity<List<VehicleRequest>> getTodayRequests() {
        List<VehicleRequest> todayRequests = vehicleRequestService.getTodayVehicleRequests();
        return ResponseEntity.ok(todayRequests);
    }

    @PostMapping("/admin/send-today-trips-email")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public ResponseEntity<String> sendTodayTripsEmail() {
        try {
            vehicleRequestService.sendTodayTripsToAdmin();
            return ResponseEntity.ok("Today's Trips list was successfully emailed to Admin.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/start-trip/{id}")
    @PreAuthorize("hasAnyRole('DRIVER', 'VEHICLE_ADMIN')")
    public ResponseEntity<VehicleRequest> startTrip(@PathVariable String id) {
        VehicleRequest startedRequest = vehicleRequestService.startTrip(id);
        return ResponseEntity.ok(startedRequest);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateVehicleRequest(
            @PathVariable("id") String requestId,
            @RequestParam("email") String employeeEmail,
            @RequestBody VehicleRequestUpdateDTO dto) {
        try {
            VehicleRequest updatedRequest = vehicleRequestService.updateVehicleRequestByEmployee(requestId, employeeEmail, dto);
            return ResponseEntity.ok(updatedRequest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}