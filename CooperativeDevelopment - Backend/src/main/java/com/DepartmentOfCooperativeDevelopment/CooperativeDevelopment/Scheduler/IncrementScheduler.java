package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Scheduler;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncrementScheduler {

    private final UserService userService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void runDailyIncrementCheck() {
        System.out.println("Starting daily automated increment check...");
        userService.processAutomatedIncrementCheck();
        System.out.println("Daily automated increment check finished.");
    }
}