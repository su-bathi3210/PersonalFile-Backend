package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Departments")
public class Department {
    @Id
    private String id;
    private String name;
    private String description;
}