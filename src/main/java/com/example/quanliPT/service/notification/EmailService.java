package com.example.quanliPT.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void sendResetEmail(String toEmail, String token, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setSubject("Smart Phòng Trọ - Mã đặt lại mật khẩu");
            helper.setText(buildResetEmailHtml(token, fullName), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể tạo email đặt lại mật khẩu", e);
        }
    }

    private String buildResetEmailHtml(String token, String fullName) {
        String displayName = fullName == null || fullName.isBlank() ? "bạn" : fullName;
        String safeDisplayName = HtmlUtils.htmlEscape(displayName);
        String safeToken = HtmlUtils.htmlEscape(token);
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.6;color:#222">
                    <h2>Đặt lại mật khẩu Smart Phòng Trọ</h2>
                    <p>Xin chào %s,</p>
                    <p>Mã xác thực đặt lại mật khẩu của bạn là:</p>
                    <div style="font-size:28px;font-weight:700;letter-spacing:6px;margin:18px 0;color:#1976d2">%s</div>
                    <p>Mã này có hiệu lực trong 15 phút. Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
                </div>
                """.formatted(safeDisplayName, safeToken);
    }

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void sendWelcomeEmail(String toEmail, String fullName, String roomNumber, String loginPhone, String defaultPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setSubject("Smart Phòng Trọ - Đơn thuê phòng đã được duyệt!");
            helper.setText(buildWelcomeEmailHtml(fullName, roomNumber, loginPhone, defaultPassword), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể tạo email thông báo duyệt", e);
        }
    }

    private String buildWelcomeEmailHtml(String fullName, String roomNumber, String loginPhone, String defaultPassword) {
        String displayName = fullName == null || fullName.isBlank() ? "Quý khách" : HtmlUtils.htmlEscape(fullName);
        String safeRoom = HtmlUtils.htmlEscape(roomNumber);
        
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f8fafc; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0;">
                    <div style="background: linear-gradient(135deg, #0f766e 0%%, #0d9488 100%%); padding: 30px 20px; text-align: center;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 0.5px;">SMART PHÒNG TRỌ</h1>
                        <p style="color: #ccfbf1; margin: 10px 0 0 0; font-size: 16px;">Thông báo duyệt hợp đồng</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #0f766e; margin-top: 0; font-size: 20px;">Chúc mừng %s! 🎉</h2>
                        <p style="color: #334155; font-size: 16px; line-height: 1.6;">
                            Đơn đăng ký thuê <strong>Phòng %s</strong> của bạn đã được Ban quản lý phê duyệt thành công. Hợp đồng điện tử đã được tạo trên hệ thống.
                        </p>
                        
                        <div style="background-color: #ffffff; border-left: 4px solid #0f766e; padding: 20px; margin: 25px 0; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
                            <h3 style="margin-top: 0; color: #1e293b; font-size: 16px;">THÔNG TIN ĐĂNG NHẬP</h3>
                            <p style="color: #64748b; margin-bottom: 15px; font-size: 14px;">Bạn có thể sử dụng thông tin sau để đăng nhập vào hệ thống, xem chi tiết hợp đồng và theo dõi hóa đơn hàng tháng:</p>
                            
                            <table style="width: 100%%; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; width: 120px;"><strong>Tên đăng nhập:</strong></td>
                                    <td style="padding: 8px 0; color: #0f766e; font-weight: bold; font-size: 16px;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;"><strong>Mật khẩu:</strong></td>
                                    <td style="padding: 8px 0; color: #ef4444; font-weight: bold; font-size: 16px; letter-spacing: 2px;">%s</td>
                                </tr>
                            </table>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; font-style: italic;">
                            * Vui lòng đăng nhập và đổi mật khẩu ngay trong lần đầu tiên để bảo mật tài khoản.
                        </p>
                        
                        <div style="text-align: center; margin-top: 35px;">
                            <a href="http://localhost:3000/login" style="background-color: #0f766e; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 12px rgba(15,118,110,0.3);">Đăng nhập ngay</a>
                        </div>
                    </div>
                    
                    <div style="background-color: #e2e8f0; padding: 20px; text-align: center; color: #64748b; font-size: 12px;">
                        <p style="margin: 0;">© 2026 Smart Phòng Trọ. Tất cả các quyền được bảo lưu.</p>
                        <p style="margin: 5px 0 0 0;">Đây là email tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
                """.formatted(displayName, safeRoom, HtmlUtils.htmlEscape(loginPhone), HtmlUtils.htmlEscape(defaultPassword));
    }

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void sendExistingUserContractEmail(String toEmail, String fullName, String roomNumber) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setSubject("Smart Phòng Trọ - Đơn thuê phòng đã được duyệt!");
            helper.setText(buildExistingUserContractEmailHtml(fullName, roomNumber), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể tạo email thông báo duyệt", e);
        }
    }

    private String buildExistingUserContractEmailHtml(String fullName, String roomNumber) {
        String displayName = fullName == null || fullName.isBlank() ? "Quý khách" : HtmlUtils.htmlEscape(fullName);
        String safeRoom = HtmlUtils.htmlEscape(roomNumber);
        
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f8fafc; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0;">
                    <div style="background: linear-gradient(135deg, #0f766e 0%%, #0d9488 100%%); padding: 30px 20px; text-align: center;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 0.5px;">SMART PHÒNG TRỌ</h1>
                        <p style="color: #ccfbf1; margin: 10px 0 0 0; font-size: 16px;">Thông báo duyệt hợp đồng</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #0f766e; margin-top: 0; font-size: 20px;">Chúc mừng %s! 🎉</h2>
                        <p style="color: #334155; font-size: 16px; line-height: 1.6;">
                            Đơn đăng ký thuê <strong>Phòng %s</strong> của bạn đã được Ban quản lý phê duyệt thành công. Hợp đồng điện tử đã được tạo trên hệ thống.
                        </p>
                        
                        <div style="background-color: #ffffff; border-left: 4px solid #0f766e; padding: 20px; margin: 25px 0; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
                            <h3 style="margin-top: 0; color: #1e293b; font-size: 16px;">THÔNG TIN ĐĂNG NHẬP</h3>
                            <p style="color: #64748b; margin-bottom: 0; font-size: 14px;">Bạn đã có sẵn tài khoản trên hệ thống. Vui lòng đăng nhập bằng <strong>số điện thoại</strong> và <strong>mật khẩu hiện tại</strong> của bạn để xem chi tiết hợp đồng và theo dõi hóa đơn hàng tháng.</p>
                        </div>
                        
                        <div style="text-align: center; margin-top: 35px;">
                            <a href="http://localhost:3000/login" style="background-color: #0f766e; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 12px rgba(15,118,110,0.3);">Đăng nhập ngay</a>
                        </div>
                    </div>
                    
                    <div style="background-color: #e2e8f0; padding: 20px; text-align: center; color: #64748b; font-size: 12px;">
                        <p style="margin: 0;">© 2026 Smart Phòng Trọ. Tất cả các quyền được bảo lưu.</p>
                        <p style="margin: 5px 0 0 0;">Đây là email tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
                """.formatted(displayName, safeRoom);
    }
}

