package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Controller;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.IncrementForm;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Notification;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.IncrementFormRepository;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/increment-form")
@RequiredArgsConstructor
@CrossOrigin
public class IncrementFormController {

    private final IncrementFormRepository incrementFormRepository;
    
    private final NotificationRepository notificationRepository;

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('PERSONALFILE_ADMIN','EMPLOYEE')")
    public IncrementForm submitIncrementForm(@RequestBody IncrementForm form) {

        IncrementForm savedForm = incrementFormRepository.save(form);

        Notification notification = notificationRepository
                .findById(form.getNotificationId())
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        notification.setStatus("COMPLETED");
        notificationRepository.save(notification);
        return savedForm;
    }

    @GetMapping("/all-submitted")
    @PreAuthorize("hasRole('PERSONALFILE_ADMIN')")
    public List<IncrementForm> getAllSubmittedForms() {
        return incrementFormRepository.findBySubmittedTrue();
    }
}