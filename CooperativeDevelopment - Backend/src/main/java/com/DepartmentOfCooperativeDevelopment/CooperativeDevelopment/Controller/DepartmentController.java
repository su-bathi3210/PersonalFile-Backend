package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Department;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @PostMapping("/add")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<Department> addDepartment(@RequestBody Department department) {
        return ResponseEntity.ok(departmentRepository.save(department));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable String id) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Error: Department not found!");
        }
        departmentRepository.deleteById(id);
        return ResponseEntity.ok().body("✅ Department deleted successfully!");
    }
}