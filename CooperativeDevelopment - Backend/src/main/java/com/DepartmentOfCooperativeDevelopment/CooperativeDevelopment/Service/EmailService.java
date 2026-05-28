package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

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
}