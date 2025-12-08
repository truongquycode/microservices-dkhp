package com.truongquycode.identity_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // Dùng @Async để việc gửi mail chạy ngầm, không làm Admin phải chờ lâu
    @Async
    public void sendPasswordResetNotification(String toEmail, String username, String newPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("Thông báo thay đổi mật khẩu - Hệ thống Quản lý Sinh viên");
            message.setText("Chào bạn,\n\n" +
                    "Mật khẩu cho tài khoản sinh viên " + username + " của bạn vừa được Quản trị viên thay đổi.\n\n" +
                    "Mật khẩu mới là: " + newPassword + "\n\n" +
                    "Vui lòng đăng nhập và đổi lại mật khẩu ngay nếu cần thiết.\n\n" +
                    "Trân trọng,\nAdmin Team.");

            mailSender.send(message);
            System.out.println("Email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }
}