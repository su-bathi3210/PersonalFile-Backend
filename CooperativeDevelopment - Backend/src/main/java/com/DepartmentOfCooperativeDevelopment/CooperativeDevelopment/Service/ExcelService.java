package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.Role;
import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.ZoneId;
import java.util.*;

@Service
public class ExcelService {

    public List<User> parseExcel(InputStream is, PasswordEncoder passwordEncoder) {
        List<User> users = new ArrayList<>();
        Set<String> seenNicsInExcel = new HashSet<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMap = new HashMap<>();
            DataFormatter formatter = new DataFormatter();

            for (int i = 0; i <= 1; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    for (Cell cell : row) {
                        String cellValue = formatter.formatCellValue(cell).trim();
                        if (!cellValue.isEmpty()) {
                            headerMap.put(cellValue, cell.getColumnIndex());
                        }
                    }
                }
            }

            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next();
            if (rows.hasNext()) rows.next();

            while (rows.hasNext()) {
                Row row = rows.next();

                String empName = getVal(row, headerMap, "Name Of The Employee");
                if (empName == null || empName.isEmpty()) continue;

                String nic = getVal(row, headerMap, "National ID");
                if (nic != null) {
                    nic = nic.trim();
                }

                if (nic != null && !nic.isEmpty()) {
                    if (seenNicsInExcel.contains(nic)) {
                        System.out.println("⚠️ Skipping row in Excel: Duplicate NIC inside the file -> " + nic);
                        continue;
                    }
                    seenNicsInExcel.add(nic);
                }

                User user = User.builder()
                        .username(empName)
                        .email(getVal(row, headerMap, "Email"))
                        .password(passwordEncoder.encode("123"))
                        .roles(Set.of(Role.EMPLOYEE))
                        .nic(nic)
                        .phoneNumber(getVal(row, headerMap, "Phone Number"))
                        .address(getVal(row, headerMap, "Address"))
                        .dateOfBirth(getDateVal(row, headerMap, "Date Of Birth"))
                        .gender(getVal(row, headerMap, "Gender"))
                        .serviceNumber(getVal(row, headerMap, "Service Number"))
                        .wnopNumber(getVal(row, headerMap, "WNOP Number"))
                        .designation(getVal(row, headerMap, "Designation"))
                        .department(getVal(row, headerMap, "Department"))
                        .dutyPlace(getVal(row, headerMap, "Duty Place"))
                        .salaryScale(getVal(row, headerMap, "Salary Scale"))
                        .dateOfFirstAppointment(getDateVal(row, headerMap, "Date Of First Appointment"))
                        .dateOfLanguageProficiency(getDateVal(row, headerMap, "Date Of Language Proficiency"))
                        .appointmentDateToPresentStatus(getDateVal(row, headerMap, "Appointment Date To Present Status"))
                        .incrementDate(getDateVal(row, headerMap, "Increment Date"))
                        .dateOfCompulsoryRetirement(getDateVal(row, headerMap, "Date Of Compulsory Retirement"))
                        .presentStatusDate(getDateVal(row, headerMap, "Present Status Date"))
                        .grade(getVal(row, headerMap, "Grade"))
                        .dateOfReceiptOfRelevantGrade(getDateVal(row, headerMap, "Date Of Receipt Of Relevant Grade"))
                        .dateOfReceiptGradeIII(getDateVal(row, headerMap, "III"))
                        .dateOfReceiptGradeII(getDateVal(row, headerMap, "II"))
                        .dateOfReceiptGradeI(getDateVal(row, headerMap, "I"))
                        .build();

                users.add(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Excel Parsing Error: " + e.getMessage());
        }
        return users;
    }

    private String getVal(Row row, Map<String, Integer> map, String colName) {
        Integer idx = map.get(colName);
        if (idx == null) return "";
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return new DataFormatter().formatCellValue(cell).trim();
    }

    private java.time.LocalDate getDateVal(Row row, Map<String, Integer> map, String colName) {
        Integer idx = map.get(colName);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        try {
            String val = new DataFormatter().formatCellValue(cell).trim();
            if (!val.isEmpty()) {
                if (val.contains("/")) {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy");
                    return java.time.LocalDate.parse(val, formatter);
                }
                return java.time.LocalDate.parse(val);
            }
        } catch (Exception e) {
            System.err.println("Error parsing date for column [" + colName + "]: " + e.getMessage());
            return null;
        }
        return null;
    }
}