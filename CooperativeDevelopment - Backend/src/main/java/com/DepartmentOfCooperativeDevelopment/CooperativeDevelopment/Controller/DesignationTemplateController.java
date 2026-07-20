package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/designation-templates")
@RequiredArgsConstructor
public class DesignationTemplateController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<?> saveDesignationTemplates(@RequestBody Map<String, Object> request) {
        String designation = (String) request.get("designation");
        @SuppressWarnings("unchecked")
        List<String> templateNames = (List<String>) request.get("templateNames");

        if (designation == null || designation.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "It is mandatory to include the designation."));
        }
        if (templateNames == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "The Templates list cannot be empty."));
        }

        userService.saveDesignationTemplates(designation, templateNames);

        return ResponseEntity.ok(Map.of("message", "Templates for the position were successfully updated."));
    }

    @GetMapping("/{designation}")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public ResponseEntity<List<String>> getTemplatesByDesignation(@PathVariable String designation) {
        List<String> templates = userService.getTemplatesForDesignation(designation);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/test-trigger")
    public ResponseEntity<?> testTriggerScheduler() {
        userService.processAutomatedIncrementCheck();
        return ResponseEntity.ok(Map.of("message", "Automated check triggered successfully for testing!"));
    }

    @GetMapping("/test-increment-flow")
    public ResponseEntity<?> testIncrementFlow(@RequestParam String email) {
        try {
            User employee = userService.findByEmail(email);

            List<String> templates = userService.getTemplatesForDesignation(employee.getDesignation());

            if (templates.isEmpty()) {
                return ResponseEntity.badRequest().body("❌ මේ සේවකයාගේ තනතුර (" + employee.getDesignation() + ") සඳහා කිසිදු Template එකක් Map කර නොමැත! කලින් පියවරේ හදපු Dashboard එකෙන් Template එකක් Map කරන්න.");
            }

            userService.sendIncrementNotification(employee.getId(), templates);

            return ResponseEntity.ok("✅ ටෙස්ට් කිරීම සාර්ථකයි! Designation: " + employee.getDesignation() + " | Templates: " + templates + " | ඊමේල් සහ Dashboard දත්ත යවන ලදී.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }
}