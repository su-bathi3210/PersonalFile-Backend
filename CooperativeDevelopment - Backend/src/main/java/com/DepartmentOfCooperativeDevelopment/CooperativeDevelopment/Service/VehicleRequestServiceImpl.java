package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleApprovalDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleRequestDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleRequestUpdateDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.VehicleAdminConfigRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.VehicleApprovalOfficerConfigRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.VehicleRequestRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.VehicleRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.DriverRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleRequestServiceImpl implements VehicleRequestService {

    private final VehicleRequestRepository repository;

    private final VehicleRepository vehicleRepository;

    private final DriverRepository driverRepository;

    private final EmailService emailService;

    private final VehicleAdminConfigRepository adminConfigRepository;

    private final VehicleApprovalOfficerConfigRepository approvalOfficerConfigRepository;

    @Override
    public void updateVehicleAdminEmail(String email) {
        VehicleAdminConfig config = adminConfigRepository.findById("VEHICLE_ADMIN_SETTINGS")
                .orElse(new VehicleAdminConfig("VEHICLE_ADMIN_SETTINGS", email));
        config.setAdminEmail(email);
        adminConfigRepository.save(config);
    }

    @Override
    public String getVehicleAdminEmail() {
        return adminConfigRepository.findById("VEHICLE_ADMIN_SETTINGS")
                .map(VehicleAdminConfig::getAdminEmail)
                .orElse(null);
    }

    @Override
    public void updateVehicleApprovalOfficerEmail(String email) {
        VehicleApprovalOfficerConfig config = approvalOfficerConfigRepository.findById("VEHICLE_APPROVAL_OFFICER_SETTINGS")
                .orElse(new VehicleApprovalOfficerConfig("VEHICLE_APPROVAL_OFFICER_SETTINGS", email));
        config.setOfficerEmail(email);
        approvalOfficerConfigRepository.save(config);
    }

    @Override
    public String getVehicleApprovalOfficerEmail() {
        return approvalOfficerConfigRepository.findById("VEHICLE_APPROVAL_OFFICER_SETTINGS")
                .map(VehicleApprovalOfficerConfig::getOfficerEmail)
                .orElse(null);
    }

    @Override
    @Transactional
    public VehicleRequest createRequest(VehicleRequestDTO dto) {
        VehicleRequest request = new VehicleRequest();
        BeanUtils.copyProperties(dto, request);
        request.setStatus(RequestStatus.PENDING);
        VehicleRequest savedRequest = repository.save(request);

        String adminEmail = getVehicleAdminEmail();
        if (adminEmail != null && !adminEmail.isEmpty()) {
            try {
                emailService.sendAdminNotificationEmail(
                        adminEmail,
                        savedRequest.getRequesterName(),
                        savedRequest.getFromLocation(),
                        savedRequest.getToLocation()
                );
            } catch (Exception e) {
                System.out.println("Admin Email sending failed on Creation: " + e.getMessage());
            }
        }
        return savedRequest;
    }

    @Override
    public List<VehicleRequest> getAllPendingRequests() {
        return repository.findByStatus(RequestStatus.PENDING);
    }

    @Override
    public List<VehicleRequest> getRequestsByEmployee(String requesterEmail) {
        return repository.findByRequesterEmail(requesterEmail);
    }

    @Override
    public VehicleRequest updateRequestStatus(String requestId, RequestStatus status) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Vehicle Request not found with id: " + requestId));

        if (request.getStatus() == RequestStatus.EMPLOYEE_CANCELLED) {
            throw new RuntimeException("Error: This request has been cancelled by the employee and cannot be modified.");
        }
        request.setStatus(status);
        return repository.save(request);
    }

    @Override
    @Transactional
    public VehicleRequest approveVehicleRequest(String requestId, VehicleApprovalDTO approvalDTO) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (request.getStatus() == RequestStatus.EMPLOYEE_CANCELLED) {
            throw new RuntimeException("Error: This request has been cancelled by the employee and cannot be modified.");
        }

        java.time.LocalDateTime localStart = request.getTravelDateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay();
        java.time.LocalDateTime localEnd = localStart.plusDays(1).minusNanos(1);

        Date dayStart = Date.from(localStart.atZone(java.time.ZoneId.systemDefault()).toInstant());
        Date dayEnd = Date.from(localEnd.atZone(java.time.ZoneId.systemDefault()).toInstant());

        List<VehicleRequest> vehicleConflicts = repository.findConflictingVehicleRequestsInRange(
                approvalDTO.getVehicleId(),
                dayStart,
                dayEnd,
                requestId
        );
        if (!vehicleConflicts.isEmpty()) {
            VehicleRequest conflictingReq = vehicleConflicts.get(0);

            String vInfo = conflictingReq.getAssignedVehicle() != null ?
                    conflictingReq.getAssignedVehicle().getVehicleNumber() : "Vehicle";
            String dInfo = conflictingReq.getAssignedDriver() != null ?
                    conflictingReq.getAssignedDriver().getName() : "Driver";

            throw new RuntimeException("Error: The selected vehicle (" + vInfo + ") is already ALLOCATED for "
                    + conflictingReq.getTravelDateTime()
                    + " to Requester: " + conflictingReq.getRequesterEmail()
                    + " (Assigned Driver: " + dInfo + ")");
        }

        List<VehicleRequest> driverConflicts = repository.findConflictingDriverRequestsInRange(
                approvalDTO.getDriverId(),
                dayStart,
                dayEnd,
                requestId
        );
        if (!driverConflicts.isEmpty()) {
            VehicleRequest conflictingReq = driverConflicts.get(0);

            String dInfo = conflictingReq.getAssignedDriver() != null ?
                    conflictingReq.getAssignedDriver().getName() : "Driver";

            throw new RuntimeException("Error: The selected driver (" + dInfo + ") is already ALLOCATED for "
                    + conflictingReq.getTravelDateTime()
                    + " to Requester: " + conflictingReq.getRequesterEmail());
        }

        Vehicle vehicle = vehicleRepository.findById(approvalDTO.getVehicleId())
                .orElseThrow(() -> new RuntimeException("The vehicle could not be found."));

        Driver driver = driverRepository.findById(approvalDTO.getDriverId())
                .orElseThrow(() -> new RuntimeException("The driver could not be found."));

        vehicle.setStatus("ALLOCATED");
        vehicleRepository.save(vehicle);

        driver.setStatus("ALLOCATED");
        driverRepository.save(driver);

        request.setAssignedVehicleId(approvalDTO.getVehicleId());
        request.setAssignedDriverId(approvalDTO.getDriverId());
        request.setAssignedVehicle(vehicle);
        request.setAssignedDriver(driver);
        request.setStatus(RequestStatus.APPROVED_BY_VEHICLE_ADMIN);

        VehicleRequest updatedRequest = repository.save(request);

        String officerEmail = getVehicleApprovalOfficerEmail();
        if (officerEmail != null && !officerEmail.isEmpty()) {
            try {
                emailService.sendOfficerNotificationOnAdminApproval(
                        officerEmail,
                        updatedRequest.getId(),
                        updatedRequest.getRequesterName(),
                        vehicle.getVehicleNumber(),
                        driver.getName()
                );
            } catch (Exception e) {
                System.out.println("Officer Email failed on Admin Approval: " + e.getMessage());
            }
        }
        return updatedRequest;
    }

    @Override
    @Transactional
    public VehicleRequest approveByApprovalOfficer(String requestId, String officerRemarks) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (request.getStatus() == RequestStatus.EMPLOYEE_CANCELLED) {
            throw new RuntimeException("Error: This request has been cancelled by the employee and cannot be modified.");
        }

        if (request.getStatus() != RequestStatus.APPROVED_BY_VEHICLE_ADMIN) {
            throw new RuntimeException("This request has not yet been approved by the Vehicle Admin.");
        }

        request.setOfficerRemarks(officerRemarks);
        request.setStatus(RequestStatus.APPROVED_BY_VEHICLE_APPROVAL_OFFICER);
        VehicleRequest updatedRequest = repository.save(request);

        String adminEmail = getVehicleAdminEmail();
        if (adminEmail != null && !adminEmail.isEmpty()) {
            try {
                emailService.sendAdminNotificationOnOfficerDecision(
                        adminEmail,
                        updatedRequest.getId(),
                        updatedRequest.getRequesterName(),
                        updatedRequest.getStatus().name(),
                        officerRemarks
                );
            } catch (Exception e) {
                System.out.println("Admin Email failed on Officer Approval: " + e.getMessage());
            }
        }
        return updatedRequest;
    }

    @Override
    public List<VehicleRequest> getRequestsByStatus(RequestStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    @Transactional
    public VehicleRequest rejectByAdmin(String requestId, String adminRemarks) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (request.getStatus() == RequestStatus.EMPLOYEE_CANCELLED) {
            throw new RuntimeException("Error: This request has been cancelled by the employee and cannot be modified.");
        }

        if (request.getStatus() == RequestStatus.TRIP_STARTED || request.getStatus() == RequestStatus.COMPLETED) {
            throw new RuntimeException("Error: Cannot cancel a trip that has already started or completed.");
        }

        releaseResources(request);

        request.setAdminRemarks(adminRemarks);
        request.setStatus(RequestStatus.REJECTED_BY_VEHICLE_ADMIN);
        VehicleRequest updatedRequest = repository.save(request);

        String officerEmail = getVehicleApprovalOfficerEmail();
        if (officerEmail != null && !officerEmail.isEmpty()) {
            try {
                emailService.sendOfficerNotificationOnAdminReject(
                        officerEmail,
                        updatedRequest.getId(),
                        updatedRequest.getRequesterName(),
                        adminRemarks
                );
            } catch (Exception e) {
                System.out.println("Officer Email failed on Admin Rejection: " + e.getMessage());
            }
        }

        return updatedRequest;
    }

    @Override
    @Transactional
    public VehicleRequest rejectByOfficer(String requestId, String officerRemarks) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (request.getStatus() == RequestStatus.EMPLOYEE_CANCELLED) {
            throw new RuntimeException("Error: This request has been cancelled by the employee and cannot be modified.");
        }

        releaseResources(request);

        request.setOfficerRemarks(officerRemarks);
        request.setStatus(RequestStatus.REJECTED_BY_VEHICLE_APPROVAL_OFFICER);
        VehicleRequest updatedRequest = repository.save(request);

        String adminEmail = getVehicleAdminEmail();
        if (adminEmail != null && !adminEmail.isEmpty()) {
            try {
                emailService.sendAdminNotificationOnOfficerDecision(
                        adminEmail,
                        updatedRequest.getId(),
                        updatedRequest.getRequesterName(),
                        updatedRequest.getStatus().name(),
                        officerRemarks
                );
            } catch (Exception e) {
                System.out.println("Admin Email failed on Officer Rejection: " + e.getMessage());
            }
        }
        return updatedRequest;
    }

    @Override
    public List<VehicleRequest> getOfficerApprovedRequests() {
        return repository.findByStatusIn(Arrays.asList(
                RequestStatus.APPROVED_BY_VEHICLE_APPROVAL_OFFICER,
                RequestStatus.TRIP_PROCESS_CONFIRMED
        ));
    }

    @Override
    @Transactional
    public VehicleRequest completeVehicleRequest(String requestId) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (request.getStatus() == RequestStatus.EMPLOYEE_CANCELLED) {
            throw new RuntimeException("Error: This request has been cancelled by the employee.");
        }

        if (request.getStatus() != RequestStatus.APPROVED_BY_VEHICLE_APPROVAL_OFFICER) {
            throw new RuntimeException("This request has not been approved by the Officer yet.");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getAssignedVehicleId())
                .orElseThrow(() -> new RuntimeException("Assigned Vehicle not found."));

        Driver driver = driverRepository.findById(request.getAssignedDriverId())
                .orElseThrow(() -> new RuntimeException("Assigned Driver not found."));

        try {
            emailService.sendVehicleAssignmentEmail(
                    request.getRequesterEmail(),
                    request.getRequesterName(),
                    vehicle.getVehicleNumber(),
                    vehicle.getManufacturer() + " " + vehicle.getModel(),
                    driver.getName(),
                    driver.getPhoneNumber()
            );
        } catch (Exception e) {
            System.out.println("Email sending failed: " + e.getMessage());
        }
        request.setStatus(RequestStatus.TRIP_PROCESS_CONFIRMED);
        return repository.save(request);
    }

    @Override
    @Transactional
    public VehicleRequest startTrip(String id) {
        VehicleRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("The requested vehicle could not be found. ID: " + id));

        if (request.getStatus() != RequestStatus.TRIP_PROCESS_CONFIRMED) {
            throw new RuntimeException("This journey can only be started after approval by the Admin.");
        }

        if (request.getAssignedVehicleId() != null) {
            vehicleRepository.findById(request.getAssignedVehicleId()).ifPresent(v -> {
                v.setStatus("BOOKED");
                vehicleRepository.save(v);
            });
        }

        if (request.getAssignedDriverId() != null) {
            driverRepository.findById(request.getAssignedDriverId()).ifPresent(d -> {
                d.setStatus("BOOKED");
                driverRepository.save(d);
            });
        }

        request.setStatus(RequestStatus.TRIP_STARTED);
        return repository.save(request);
    }

    @Override
    @Transactional
    public VehicleRequest endVehicleTrip(String requestId) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (request.getStatus() != RequestStatus.TRIP_STARTED) {
            throw new RuntimeException("This journey has not yet begun or has already ended.");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getAssignedVehicleId())
                .orElseThrow(() -> new RuntimeException("Assigned Vehicle not found."));
        vehicle.setStatus("AVAILABLE");
        vehicleRepository.save(vehicle);

        Driver driver = driverRepository.findById(request.getAssignedDriverId())
                .orElseThrow(() -> new RuntimeException("Assigned Driver not found."));
        driver.setStatus("AVAILABLE");
        driverRepository.save(driver);

        request.setStatus(RequestStatus.COMPLETED);
        return repository.save(request);
    }

    @Override
    public List<VehicleRequest> getAllRequestsForAdmin() {
        return repository.findAll();
    }

    @Override
    public List<VehicleRequest> getDriverDashboardTrips(String driverPhoneNumber) {
        Driver driver = driverRepository.findByPhoneNumber(driverPhoneNumber)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        return repository.findByAssignedDriverId(driver.getId());
    }

    public List<VehicleRequest> getDriverDashboardTripsByNic(String nic) {
        Driver driver = driverRepository.findByNic(nic)
                .orElseThrow(() -> new RuntimeException("Driver not found with NIC: " + nic));
        return repository.findByAssignedDriverId(driver.getId());
    }

    private void releaseResources(VehicleRequest request) {
        if (request.getAssignedVehicleId() != null) {
            vehicleRepository.findById(request.getAssignedVehicleId()).ifPresent(v -> {
                v.setStatus("AVAILABLE");
                vehicleRepository.save(v);
            });
        }
        if (request.getAssignedDriverId() != null) {
            driverRepository.findById(request.getAssignedDriverId()).ifPresent(d -> {
                d.setStatus("AVAILABLE");
                driverRepository.save(d);
            });
        }
    }

    @Override
    @Transactional
    public VehicleRequest cancelRequestByEmployee(String requestId, String employeeEmail) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (!request.getRequesterEmail().equalsIgnoreCase(employeeEmail)) {
            throw new RuntimeException("Error: You are not authorized to cancel this request!");
        }

        if (request.getStatus() == RequestStatus.TRIP_STARTED || request.getStatus() == RequestStatus.COMPLETED) {
            throw new RuntimeException("Error: Cannot cancel a trip that has already started or completed.");
        }

        releaseResources(request);

        request.setStatus(RequestStatus.EMPLOYEE_CANCELLED);
        VehicleRequest cancelledRequest = repository.save(request);

        String adminEmail = getVehicleAdminEmail();
        if (adminEmail != null && !adminEmail.isEmpty()) {
            try {
                emailService.sendAdminNotificationOnEmployeeCancel(
                        adminEmail,
                        cancelledRequest.getId(),
                        cancelledRequest.getRequesterName(),
                        "The employee canceled through the system."
                );
            } catch (Exception e) {
                System.out.println("Admin Email failed on Employee Cancel: " + e.getMessage());
            }
        }

        String officerEmail = getVehicleApprovalOfficerEmail();
        if (officerEmail != null && !officerEmail.isEmpty()) {
            try {
                emailService.sendOfficerNotificationOnEmployeeCancel(
                        officerEmail,
                        cancelledRequest.getId(),
                        cancelledRequest.getRequesterName()
                );
            } catch (Exception e) {
                System.out.println("Officer Email failed on Employee Cancel: " + e.getMessage());
            }
        }
        return cancelledRequest;
    }

    @Override
    public long getPendingRequestsCountForAdmin() {
        return repository.countByStatus(RequestStatus.PENDING);
    }

    @Override
    public long getOfficerApprovedRequestsCountForAdmin() {
        return repository.countByStatus(RequestStatus.APPROVED_BY_VEHICLE_APPROVAL_OFFICER);
    }

    @Override
    public List<VehicleRequest> getTodayVehicleRequests() {
        java.time.LocalDateTime startOfDayLocal = java.time.LocalDate.now().atStartOfDay();
        java.util.Date startOfDay = java.util.Date.from(startOfDayLocal.atZone(java.time.ZoneId.systemDefault()).toInstant());

        java.time.LocalDateTime endOfDayLocal = java.time.LocalDate.now().atTime(23, 59, 59, 999);
        java.util.Date endOfDay = java.util.Date.from(endOfDayLocal.atZone(java.time.ZoneId.systemDefault()).toInstant());

        return repository.findByTravelDateTimeBetween(startOfDay, endOfDay);
    }

    @Override
    @Transactional
    public void sendTodayTripsToAdmin() {
        String adminEmail = getVehicleAdminEmail();

        if (adminEmail == null || adminEmail.isEmpty()) {
            throw new RuntimeException("The admin email is not saved in the system.");
        }

        java.time.LocalDateTime startOfDayLocal = java.time.LocalDate.now().atStartOfDay();
        java.util.Date startOfDay = java.util.Date.from(startOfDayLocal.atZone(java.time.ZoneId.systemDefault()).toInstant());

        java.time.LocalDateTime endOfDayLocal = java.time.LocalDate.now().atTime(23, 59, 59, 999);
        java.util.Date endOfDay = java.util.Date.from(endOfDayLocal.atZone(java.time.ZoneId.systemDefault()).toInstant());

        List<VehicleRequest> todayRequests = repository.findByTravelDateTimeBetween(startOfDay, endOfDay);

        emailService.sendTodayTripsNotificationEmail(adminEmail, todayRequests);
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 6 * * ?")
    public void autoSendTodayTrips() {
        String adminEmail = getVehicleAdminEmail();

        java.time.LocalDateTime startOfDayLocal = java.time.LocalDate.now().atStartOfDay();
        java.util.Date startOfDay = java.util.Date.from(startOfDayLocal.atZone(java.time.ZoneId.systemDefault()).toInstant());

        java.time.LocalDateTime endOfDayLocal = java.time.LocalDate.now().atTime(23, 59, 59, 999);
        java.util.Date endOfDay = java.util.Date.from(endOfDayLocal.atZone(java.time.ZoneId.systemDefault()).toInstant());

        List<VehicleRequest> todayRequests = repository.findByTravelDateTimeBetween(startOfDay, endOfDay);

        if (adminEmail != null && !adminEmail.isEmpty()) {
            try {
                emailService.sendTodayTripsNotificationEmail(adminEmail, todayRequests);
                System.out.println("Scheduler Success: Today's report emailed to Admin.");
            } catch (Exception e) {
                System.out.println("Scheduler Admin Email Error: " + e.getMessage());
            }
        }

        if (todayRequests != null && !todayRequests.isEmpty()) {
            for (VehicleRequest request : todayRequests) {

                if (request.getStatus() == RequestStatus.TRIP_PROCESS_CONFIRMED ||
                        request.getStatus() == RequestStatus.TRIP_STARTED) {

                    String timeStr = request.getTravelDateTime() != null ? request.getTravelDateTime().toString() : "Today";

                    if (request.getRequesterEmail() != null && !request.getRequesterEmail().isEmpty()) {
                        try {
                            emailService.sendTodayTripReminderToEmployee(
                                    request.getRequesterEmail(),
                                    request.getRequesterName(),
                                    request.getFromLocation(),
                                    request.getToLocation(),
                                    timeStr,
                                    request.getAssignedDriver() != null ? request.getAssignedDriver().getName() : "Unknown",
                                    request.getAssignedDriver() != null ? request.getAssignedDriver().getPhoneNumber() : "N/A"
                            );
                            System.out.println("Reminder sent to Employee: " + request.getRequesterEmail());
                        } catch (Exception e) {
                            System.out.println("Failed to send reminder to Employee: " + e.getMessage());
                        }
                    }

                    if (request.getAssignedDriver() != null) {
                        Driver driver = request.getAssignedDriver();
                        if (driver.getEmail() != null && !driver.getEmail().isEmpty()) {
                            try {
                                emailService.sendTodayTripReminderToDriver(
                                        driver.getEmail(),
                                        driver.getName(),
                                        request.getFromLocation(),
                                        request.getToLocation(),
                                        timeStr,
                                        request.getRequesterName()
                                );
                                System.out.println("Reminder sent to Driver: " + driver.getEmail());
                            } catch (Exception e) {
                                System.out.println("Failed to send reminder to Driver: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public VehicleRequest updateVehicleRequestByEmployee(String requestId, String employeeEmail, VehicleRequestUpdateDTO dto) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (!request.getRequesterEmail().equalsIgnoreCase(employeeEmail)) {
            throw new RuntimeException("Error: You are not authorized to update this request!");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Error: Only PENDING requests can be updated by the employee.");
        }

        if (dto.getFromLocation() != null) request.setFromLocation(dto.getFromLocation());
        if (dto.getToLocation() != null) request.setToLocation(dto.getToLocation());
        if (dto.getDistanceKm() != null) request.setDistanceKm(dto.getDistanceKm());
        if (dto.getTravelDateTime() != null) request.setTravelDateTime(dto.getTravelDateTime());
        if (dto.getDutyNature() != null) request.setDutyNature(dto.getDutyNature());
        if (dto.getReason() != null) request.setReason(dto.getReason());
        if (dto.getPhoneNumber() != null) request.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getRequesterPosition() != null) request.setRequesterPosition(dto.getRequesterPosition());
        if (dto.getDepartment() != null) request.setDepartment(dto.getDepartment());

        return repository.save(request);
    }
}