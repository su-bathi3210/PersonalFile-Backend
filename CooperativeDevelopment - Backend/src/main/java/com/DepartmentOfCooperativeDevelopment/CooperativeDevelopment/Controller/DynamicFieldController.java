package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.DynamicField;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Role;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.DynamicFieldRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.UserRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.UserService; // 👈 අලුතින් එකතු කළා
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dynamic-fields")
@RequiredArgsConstructor
public class DynamicFieldController {

    private final DynamicFieldRepository dynamicFieldRepository;

    private final UserRepository userRepository;

    private final UserService userService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> addField(@RequestBody DynamicField dynamicField) {
        if (dynamicFieldRepository.existsByFieldKey(dynamicField.getFieldKey())) {
            return ResponseEntity.badRequest().body("Error: Field key already exists!");
        }
        return ResponseEntity.ok(userService.createDynamicField(dynamicField));
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

        config.setScope(updateData.getScope());
        config.setTargetDesignations(updateData.getTargetDesignations());

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

    @GetMapping("/designations-summary")
    public ResponseEntity<List<Map<String, Object>>> getDesignationsSummary() {
        List<User> employees = userRepository.findByRolesContaining(Role.EMPLOYEE);
        Map<String, Integer> countMap = new HashMap<>();

        for (User user : employees) {
            String des = user.getDesignation();
            if (des != null && !des.trim().isEmpty()) {
                countMap.put(des.trim(), countMap.getOrDefault(des.trim(), 0) + 1);
            }
        }

        List<Map<String, Object>> summary = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("designation", entry.getKey());
            item.put("employeeCount", entry.getValue());
            summary.add(item);
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/employees-by-designation")
    public ResponseEntity<List<User>> getEmployeesByDesignation(@RequestParam String designation) {
        List<User> allEmployees = userRepository.findByRolesContaining(Role.EMPLOYEE);
        List<User> filtered = allEmployees.stream()
                .filter(u -> u.getDesignation() != null && u.getDesignation().trim().equalsIgnoreCase(designation.trim()))
                .toList();
        return ResponseEntity.ok(filtered);
    }
}