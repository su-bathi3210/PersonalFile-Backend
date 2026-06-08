package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.DynamicField;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.DynamicFieldRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dynamic-fields")
@RequiredArgsConstructor
public class DynamicFieldController {

    private final DynamicFieldRepository dynamicFieldRepository;

    private final UserRepository userRepository;

    @PostMapping("/add")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> addField(@RequestBody DynamicField dynamicField) {
        if (dynamicFieldRepository.existsByFieldKey(dynamicField.getFieldKey())) {
            return ResponseEntity.badRequest().body("Error: Field key already exists!");
        }
        return ResponseEntity.ok(dynamicFieldRepository.save(dynamicField));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> updateField(@PathVariable String id, @RequestBody DynamicField updateData) {
        DynamicField config = dynamicFieldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Field config not found"));

        config.setDisplayName(updateData.getDisplayName());
        config.setFieldType(updateData.getFieldType());
        config.setRequired(updateData.isRequired());
        config.setGlobal(updateData.isGlobal());
        config.setEmployeeEmail(updateData.isGlobal() ? "" : updateData.getEmployeeEmail());

        config.setAdminOnly(updateData.isAdminOnly());

        return ResponseEntity.ok(dynamicFieldRepository.save(config));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> deleteField(@PathVariable String id) {
        DynamicField config = dynamicFieldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Field config not found"));

        String keyToRemove = config.getFieldKey();

        userRepository.findAll().forEach(user -> {
            if (user.getDynamicFields() != null && user.getDynamicFields().containsKey(keyToRemove)) {
                user.getDynamicFields().remove(keyToRemove);
                userRepository.save(user);
            }
        });

        dynamicFieldRepository.deleteById(id);
        return ResponseEntity.ok("✅ Field deleted from configuration and all employee profiles successfully!");
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('PERSONALFILE_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<DynamicField>> getAllFields(@RequestParam(required = false) String email) {
        if (email != null && !email.trim().isEmpty()) {
            return ResponseEntity.ok(dynamicFieldRepository.findByIsGlobalTrueOrEmployeeEmail(email));
        }
        return ResponseEntity.ok(dynamicFieldRepository.findAll());
    }
}