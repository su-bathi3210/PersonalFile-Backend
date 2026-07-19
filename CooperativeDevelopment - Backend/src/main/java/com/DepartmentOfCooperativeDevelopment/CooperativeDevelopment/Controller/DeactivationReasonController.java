package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.DeactivationReason;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.DeactivationReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deactivation-reasons")
@RequiredArgsConstructor

public class DeactivationReasonController {

    private final DeactivationReasonRepository repository;

    @PostMapping("/add")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> addReason(@RequestBody DeactivationReason reason) {
        if (reason.getType() == null || (!reason.getType().equals("ACTIVATE") && !reason.getType().equals("DEACTIVATE"))) {
            return ResponseEntity.badRequest().body("Invalid type. Must be ACTIVATE or DEACTIVATE");
        }
        return ResponseEntity.ok(repository.save(reason));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<List<DeactivationReason>> getReasonsByType(@PathVariable String type) {
        return ResponseEntity.ok(repository.findByType(type.toUpperCase()));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> deleteReason(@PathVariable String id) {
        repository.deleteById(id);
        return ResponseEntity.ok("Reason deleted successfully!");
    }
}