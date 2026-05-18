package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "IncrementForms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncrementForm {

    @Id
    private String id;
    private String userId;

    private String notificationId;

    private String headOfficeFileNumber;

    private String officerName;
    private String grade;
    private String assistantCommissionerDivision;
    private String transferDateToACoffice;

    private String incrementDate;
    private String currentSalary;
    private String incrementAmount;
    private String totalWithIncrement;
    private String monthlyConsolidatedSalary;

    private String salaryIncrementSuspendedDetails;
    private String sickLeaveCount;

    private String passedSecondLanguageTest;
    private String passedFirstInspectorExam;
    private String examPassedDateAndYear;

    private String efficiencyBarReached;

    private String disciplinaryActionsDetails;
    private String warningsOrPunishmentsDetails;

    private boolean submitted;
}