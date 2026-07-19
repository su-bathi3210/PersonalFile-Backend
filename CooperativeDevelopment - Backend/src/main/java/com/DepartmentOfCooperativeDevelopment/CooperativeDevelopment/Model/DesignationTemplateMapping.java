package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "DesignationTemplateMapping")
public class DesignationTemplateMapping {
    @Id
    private String id;
    private String designation;
    private List<String> templateNames;
}