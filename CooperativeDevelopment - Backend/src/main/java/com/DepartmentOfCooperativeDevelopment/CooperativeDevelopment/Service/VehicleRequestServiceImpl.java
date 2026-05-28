package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleApprovalDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.DTO.VehicleRequestDTO;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.*;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.VehicleRequestRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.VehicleRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.DriverRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleRequestServiceImpl implements VehicleRequestService {

    private final VehicleRequestRepository repository;

    private final VehicleRepository vehicleRepository;

    private final DriverRepository driverRepository;

    private final EmailService emailService;

    @Override
    public VehicleRequest createRequest(VehicleRequestDTO dto) {
        VehicleRequest request = new VehicleRequest();
        BeanUtils.copyProperties(dto, request);
        request.setStatus(RequestStatus.PENDING);
        return repository.save(request);
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
        request.setStatus(status);
        return repository.save(request);
    }

    @Override
    @Transactional
    public VehicleRequest approveVehicleRequest(String requestId, VehicleApprovalDTO approvalDTO) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        Vehicle vehicle = vehicleRepository.findById(approvalDTO.getVehicleId())
                .orElseThrow(() -> new RuntimeException("The vehicle could not be found."));
        vehicle.setStatus("BOOKED");
        vehicleRepository.save(vehicle);

        Driver driver = driverRepository.findById(approvalDTO.getDriverId())
                .orElseThrow(() -> new RuntimeException("The driver could not be found."));
        driver.setStatus("BOOKED");
        driverRepository.save(driver);

        request.setAssignedVehicleId(approvalDTO.getVehicleId());
        request.setAssignedDriverId(approvalDTO.getDriverId());
        request.setAssignedVehicle(vehicle);
        request.setAssignedDriver(driver);
        request.setStatus(RequestStatus.APPROVED_BY_VEHICLE_ADMIN);

        return repository.save(request);
    }

    @Override
    @Transactional
    public VehicleRequest approveByApprovalOfficer(String requestId, String officerRemarks) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (request.getStatus() != RequestStatus.APPROVED_BY_VEHICLE_ADMIN) {
            throw new RuntimeException("This request has not yet been approved by the Vehicle Admin.");
        }

        request.setOfficerRemarks(officerRemarks);
        request.setStatus(RequestStatus.APPROVED_BY_VEHICLE_APPROVAL_OFFICER);

        return repository.save(request);
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

        releaseResources(request);

        request.setAdminRemarks(adminRemarks);
        request.setStatus(RequestStatus.REJECTED_BY_VEHICLE_ADMIN);

        return repository.save(request);
    }

    @Override
    @Transactional
    public VehicleRequest rejectByOfficer(String requestId, String officerRemarks) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        releaseResources(request);

        request.setOfficerRemarks(officerRemarks);
        request.setStatus(RequestStatus.REJECTED_BY_VEHICLE_APPROVAL_OFFICER);

        return repository.save(request);
    }

    @Override
    public List<VehicleRequest> getOfficerApprovedRequests() {
        return repository.findByStatus(RequestStatus.APPROVED_BY_VEHICLE_APPROVAL_OFFICER);
    }

    @Override
    @Transactional
    public VehicleRequest completeVehicleRequest(String requestId) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

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

        request.setStatus(RequestStatus.TRIP_STARTED);
        return repository.save(request);
    }

    @Override
    @Transactional
    public VehicleRequest endVehicleTrip(String requestId) {
        VehicleRequest request = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("The vehicle request could not be found."));

        if (request.getStatus() != RequestStatus.TRIP_STARTED) {
            throw new RuntimeException("This trip has either not started yet or is already completed.");
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
}