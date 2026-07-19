package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CooperativeDevelopmentApplication {
	public static void main(String[] args) {
		SpringApplication.run(CooperativeDevelopmentApplication.class, args);
	}
}
