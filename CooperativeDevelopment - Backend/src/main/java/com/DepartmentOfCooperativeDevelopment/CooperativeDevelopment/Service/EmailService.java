package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Model.VehicleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendOTP(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset OTP - DCD System");
        message.setText("Your OTP for password reset is: " + otp + "\nThis code will expire in 5 minutes.");
        mailSender.send(message);
    }

    public void sendIncrementReminder(String to, String name, LocalDate incrementDate) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("වාර්ෂික වැටුප් වර්ධක පෝරමය ඉදිරිපත් කිරීම - DCD System");

        String emailContent = "හිතවත් " + name + ",\n\n" +
                "ඔබගේ මීළඟ වාර්ෂික වැටුප් වර්ධක දිනය (Increment Date) " + incrementDate + " දිනට යෙදී ඇත.\n\n" +
                "එම දිනය තව මාසයක් ඇතුළත එළඹෙන බැවින්, කරුණාකර හැකි ඉක්මනින් Personal File System එකට ලොග් වී " +
                "අදාළ වැටුප් වර්ධක පෝරමය (Increment Form) සම්පූර්ණ කර ඉදිරිපත් (Submit) කරන ලෙස දන්වා සිටිමු.\n\n" +
                "ඔබට ලබා දී ඇති කාල සීමාව ඇතුළත නිවැරදිව තොරතුරු ඇතුළත් කර පෝරමය ඉදිරිපත් කිරීමට කාරුණික වන්න.\n\n" +
                "ස්තුතියි,\nපාලන අංශය.";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendVehicleAssignmentEmail(String to, String employeeName, String vehicleNo, String vehicleModel, String driverName, String driverPhone) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("වාහන වෙන්කිරීම තහවුරු කිරීම - DCD System");

        String emailContent = "හිතවත් " + employeeName + ",\n\n" +
                "ඔබ විසින් ඉදිරිපත් කරන ලද වාහන ඉල්ලුම්පත (Vehicle Request) සාර්ථකව අනුමත කර අවසන් කර ඇත.\n\n" +
                "ඔබ වෙනුවෙන් වෙන් කරන ලද වාහන සහ රියදුරු විස්තර පහත පරිදි වේ:\n" +
                "----------------------------------------\n" +
                "• වාහන අංකය (Vehicle No): " + vehicleNo + "\n" +
                "• වාහන වර්ගය (Model): " + vehicleModel + "\n" +
                "• රියදුරුගේ නම (Driver Name): " + driverName + "\n" +
                "• දුරකථන අංකය (Driver Phone): " + driverPhone + "\n" +
                "----------------------------------------\n\n" +
                "ස්තුතියි,\nපාලන අංශය (Vehicle Administration).";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendAdminNotificationEmail(String adminEmail, String employeeName, String fromLocation, String toLocation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("නව වාහන ඉල්ලුම්පතක් ලැබී ඇත - DCD System");

        String emailContent = "පාලන අංශය (Vehicle Admin) වෙත,\n\n" +
                employeeName + " විසින් පද්ධතිය වෙත නව වාහන ඉල්ලුම්පතක් (Vehicle Request) ඉදිරිපත් කර ඇත.\n\n" +
                "• ගමන් ආරම්භය: " + fromLocation + "\n" +
                "• ගමනාන්තය: " + toLocation + "\n\n" +
                "කරුණාකර පද්ධතියට (DCD System) ලොග් වී අදාළ වාහන සහ රියදුරු වෙන් කිරීම් සිදු කිරීමට කටයුතු කරන්න.\n\n" +
                "ස්තුතියි,\nSystem Notification.";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendAdminNotificationOnOfficerDecision(String adminEmail, String id, String employeeName, String status, String comment) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);

        String statusText = status.equals("APPROVED_BY_VEHICLE_APPROVAL_OFFICER") ? "අනුමත කර ඇත" : "ප්‍රතික්ෂේප කර ඇත";
        message.setSubject("වාහන ඉල්ලුම්පතක් නිලධාරී විසින් " + statusText + " - DCD System");

        String emailContent = "පාලන අංශය (Vehicle Admin) වෙත,\n\n" +
                "වාහන අනුමත කිරීමේ නිලධාරියා (Vehicle Approval Officer) විසින් පහත වාහන ඉල්ලුම්පත සඳහා තීරණයක් ලබා දී ඇත.\n\n" +
                "• ඉල්ලුම්පත් අංකය (ID): " + id + "\n" +
                "• අදාළ සේවකයා: " + employeeName + "\n" +
                "• වත්මන් තත්ත්වය (Status): " + statusText + "\n" +
                "• නිලධාරී සටහන (Comment): " + (comment != null && !comment.isEmpty() ? comment : "සටහන් නොමැත") + "\n\n" +
                "කරුණාකර පද්ධතියට ලොග් වී ඉදිරි පියවරයන් පරීක්ෂා කරන්න.\n\n" +
                "ස්තුතියි,\nSystem Notification.";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendAdminNotificationOnEmployeeCancel(String adminEmail, String id, String employeeName, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("වාහන ඉල්ලුම්පතක් සේවකයා විසින් අවලංගු කර ඇත - DCD System");

        String emailContent = "පාලන අංශය (Vehicle Admin) වෙත,\n\n" +
                "සේවකයා විසින් පද්ධතියට ඉදිරිපත් කර තිබූ වාහන ඉල්ලුම්පතක් අවලංගු (Cancel) කර ඇති බව කාරුණිකව දැනුම් දෙමු.\n\n" +
                "• ඉල්ලුම්පත් අංකය (ID): " + id + "\n" +
                "• සේවකයාගේ නම: " + employeeName + "\n" +
                "• අවලංගු කිරීමට හේතුව: " + (reason != null && !reason.isEmpty() ? reason : "සඳහන් කර නොමැත") + "\n\n" +
                "මේ හේතුවෙන් එම ඉල්ලුම්පත සඳහා වෙන් කර තිබූ වාහන සහ රියදුරන් (ඇත්නම්) පද්ධතිය විසින් ස්වයංක්‍රීයව නිදහස් (Available) කර ඇත.\n\n" +
                "ස්තුතියි,\nSystem Notification.";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendOfficerNotificationOnAdminApproval(String officerEmail, String requestId, String employeeName, String vehicleNo, String driverName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(officerEmail);
        message.setSubject("අනුමැතිය සඳහා නව වාහන ඉල්ලුම්පතක් පැමිණ ඇත - DCD System");

        String emailContent = "වාහන අනුමත කිරීමේ නිලධාරී (Vehicle Approval Officer) වෙත,\n\n" +
                "පාලන අංශය (Vehicle Admin) විසින් පහත වාහන ඉල්ලුම්පත සඳහා වාහන සහ රියදුරු වෙන් කර අනුමත කර ඇත.\n\n" +
                "• ඉල්ලුම්පත් අංකය (ID): " + requestId + "\n" +
                "• අදාළ සේවකයා: " + employeeName + "\n" +
                "• වෙන් කළ වාහනය (Vehicle No): " + vehicleNo + "\n" +
                "• වෙන් කළ රියදුරු (Driver Name): " + driverName + "\n\n" +
                "කරුණාකර පද්ධතියට ලොග් වී මෙම ඉල්ලුම්පත පරික්ෂා කර ඔබගේ අවසන් අනුමැතිය (Final Approval) ලබා දීමට කටයුතු කරන්න.\n\n" +
                "ස්තුතියි,\nSystem Notification.";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendOfficerNotificationOnAdminReject(String officerEmail, String requestId, String employeeName, String adminRemarks) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(officerEmail);
        message.setSubject("වාහන ඉල්ලුම්පතක් පරිපාලක විසින් ප්‍රතික්ෂේප කර ඇත - DCD System");

        String emailContent = "වාහන අනුමත කිරීමේ නිලධාරී වෙත,\n\n" +
                "පද්ධතියට ලැබී තිබූ පහත වාහන ඉල්ලුම්පත Vehicle Admin විසින් ප්‍රතික්ෂේප (Reject) කර ඇති බව දැනුම් දෙමු.\n\n" +
                "• ඉල්ලුම්පත් අංකය (ID): " + requestId + "\n" +
                "• සේවකයාගේ නම: " + employeeName + "\n" +
                "• පරිපාලක සටහන (Admin Remarks): " + (adminRemarks != null ? adminRemarks : "සටහන් නොමැත") + "\n\n" +
                "ස්තුතියි,\nSystem Notification.";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendOfficerNotificationOnEmployeeCancel(String officerEmail, String requestId, String employeeName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(officerEmail);
        message.setSubject("වාහන ඉල්ලුම්පතක් සේවකයා විසින් අවලංගු කර ඇත - DCD System");

        String emailContent = "වාහන අනුමත කිරීමේ නිලධාරී වෙත,\n\n" +
                "සේවකයා විසින් පද්ධතියට ඉදිරිපත් කර තිබූ වාහන ඉල්ලුම්පතක් අවලංගු (Cancel) කර ඇති බව කාරුණිකව දැනුම් දෙමු.\n\n" +
                "• ඉල්ලුම්පත් අංකය (ID): " + requestId + "\n" +
                "• සේවකයාගේ නම: " + employeeName + "\n\n" +
                "එබැවින් මෙම ඉල්ලුම්පත මත ඉදිරි ක්‍රියාමාර්ග ගැනීම අවශ්‍ය නොවේ.\n\n" +
                "ස්තුතියි,\nSystem Notification.";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendAdminNotificationOnServiceRecord(String adminEmail, String vehicleNo, String vehicleModel, String driverName, Double serviceKm, Double nextServiceKm, Double cost, String description) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("වාහන නඩත්තු වාර්තාවක් (Service Record) ඇතුළත් කිරීම - DCD System");

        String formattedCost = String.format("%,.2f", cost);

        String emailContent = "පාලන අංශය (Vehicle Admin) වෙත,\n\n" +
                "රියදුරු විසින් පද්ධතිය හරහා වාහනයක නව නඩත්තු වාර්තාවක් (Service Record) ඇතුළත් කර ඇති බව කාරුණිකව දැනුම් දෙමු.\n\n" +
                "• වාහනයේ විස්තර (Vehicle Details):\n" +
                "  - වාහන අංකය: " + vehicleNo + "\n" +
                "  - වාහන වර්ගය: " + vehicleModel + "\n\n" +
                "• නඩත්තු විස්තර (Service Details):\n" +
                "  - සර්විස් කරන ලද KM සීමාව: " + String.format("%.1f", serviceKm) + " KM\n" +
                "  - මීළඟ සර්විස් එක කල යුතු KM සීමාව: " + String.format("%.1f", nextServiceKm) + " KM\n" +
                "  - වියදම (Cost): රු. " + formattedCost + "\n" +
                "  - විස්තරය (Description): " + (description != null && !description.isEmpty() ? description : "සටහන් නොමැත") + "\n\n" +
                "• රියදුරුගේ විස්තර (Driver Details):\n" +
                "  - ඇතුළත් කළ රියදුරු: " + driverName + "\n\n" +
                "ස්තුතියි,\nSystem Notification.";

        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendTodayTripsNotificationEmail(String adminEmail, List<VehicleRequest> todayRequests) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("අද දින ධාවන කටයුතු පිළිබඳ දැනුම්දීම - Today's Vehicle Trips");

        if (todayRequests.isEmpty()) {
            message.setText("සුභ දවසක්! අද දින සඳහා කිසිදු වාහන ධාවන ඉල්ලීමක් (Vehicle Requests) නොමැත.");
        } else {
            StringBuilder text = new StringBuilder("සුභ දවසක්! අද දින ධාවනය වීමට නියමිත වාහන ලැයිස්තුව පහත දැක්වේ:\n\n");
            int count = 1;
            for (VehicleRequest req : todayRequests) {
                text.append(count).append(". ඉල්ලුම්කරු: ").append(req.getRequesterName()).append("\n")
                        .append("   ගමන් ආරම්භය: ").append(req.getFromLocation()).append("\n")
                        .append("   ගමනාන්තය: ").append(req.getToLocation()).append("\n")
                        .append("   වාහනය: ").append(req.getAssignedVehicle() != null ? req.getAssignedVehicle().getVehicleNumber() : "නොමැත").append("\n")
                        .append("   රියදුරු: ").append(req.getAssignedDriver() != null ? req.getAssignedDriver().getName() : "නොමැත").append("\n")
                        .append("   තත්ත්වය: ").append(req.getStatus().name()).append("\n\n");
                count++;
            }
            message.setText(text.toString());
        }
        mailSender.send(message);
    }

    public void sendTodayTripReminderToEmployee(String toEmail, String employeeName, String fromLoc, String toLoc, String time, String driverName, String driverPhone) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("🔔 මතක් කිරීමයි: අද දින ඔබගේ වාහන සංචාරය (Today's Vehicle Trip Reminder)");
        message.setText("හිතවත් " + employeeName + ",\n\n" +
                "ඔබ විසින් වෙන්කරවා ගන්නා ලද වාහන සංචාරය අද දින ධාවනය වීමට නියමිතව ඇති බව කාරුණිකව මතක් කර සිටිමු.\n\n" +
                "📌 සංචාරක විස්තර:\n" +
                "• ගමන් මාර්ගය: " + fromLoc + " සිට " + toLoc + " දක්වා\n" +
                "• වේලාව: " + time + "\n\n" +
                "📌 රියදුරු විස්තර:\n" +
                "• රියදුරුගේ නම: " + driverName + "\n" +
                "• දුරකථන අංකය: " + driverPhone + "\n\n" +
                "සුභ ගමනක් ප්‍රාර්ථනා කරමු!\n- සමුපකාර සංවර්ධන දෙපාර්තමේන්තුව -");
        mailSender.send(message);
    }

    public void sendTodayTripReminderToDriver(String toEmail, String driverName, String fromLoc, String toLoc, String time, String employeeName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("🔔 නිවේදනයයි: අද දින ඔබට පවරා ඇති වාහන සංචාරය (Assigned Trip for Today)");
        message.setText("හිතවත් " + driverName + ",\n\n" +
                "අද දින ඔබට ධාවනය සඳහා වාහන සංචාරයක් පවරා ඇති බව කාරුණිකව දැනුම් දෙමු.\n\n" +
                "📌 සංචාරක විස්තර:\n" +
                "• ගමන් මාර්ගය: " + fromLoc + " සිට " + toLoc + " දක්වා\n" +
                "• වේලාව: " + time + "\n" +
                "• අදාළ නිලධාරියා: " + employeeName + "\n\n" +
                "කරුණාකර නියමිත වේලාවට ගමන ආරම්භ කිරීමට සූදානම් වන්න. සුභ ගමනක්!\n- සමුපකාර සංවර්ධන දෙපාර්තමේන්තුව -");
        mailSender.send(message);
    }
}