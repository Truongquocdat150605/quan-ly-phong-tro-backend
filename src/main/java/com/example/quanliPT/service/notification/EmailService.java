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
    public void sendWelcomeEmail(com.example.quanliPT.model.Contract contract, String defaultPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(contract.getTenant().getEmail());
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setSubject("Smart Phòng Trọ - Đơn thuê phòng đã được duyệt!");
            helper.setText(buildWelcomeEmailHtml(contract, defaultPassword), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể tạo email thông báo duyệt", e);
        }
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,d", amount.longValue()).replace(',', '.');
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) return "Không thời hạn";
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String buildWelcomeEmailHtml(com.example.quanliPT.model.Contract contract, String defaultPassword) {
        String fullName = contract.getTenant().getFullName();
        String displayName = fullName == null || fullName.isBlank() ? "Quý khách" : HtmlUtils.htmlEscape(fullName);
        String safeRoom = HtmlUtils.htmlEscape(contract.getRoom().getRoomNumber());
        String rentPrice = formatCurrency(contract.getRentPrice());
        String deposit = formatCurrency(contract.getDeposit());
        String startDate = formatDate(contract.getStartDate());
        String endDate = formatDate(contract.getEndDate());
        String loginPhone = HtmlUtils.htmlEscape(contract.getTenant().getPhone());
        String safePass = HtmlUtils.htmlEscape(defaultPassword);
        
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f8fafc; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0;">
                    <div style="background: linear-gradient(135deg, #0f766e 0%%, #0d9488 100%%); padding: 30px 20px; text-align: center;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 0.5px;">SMART PHÒNG TRỌ</h1>
                        <p style="color: #ccfbf1; margin: 10px 0 0 0; font-size: 16px;">Thông báo duyệt hợp đồng</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #0f766e; margin-top: 0; font-size: 20px;">Chúc mừng %s! 🎉</h2>
                        <p style="color: #334155; font-size: 16px; line-height: 1.6;">
                            Đơn đăng ký thuê <strong>Phòng %s</strong> của bạn đã được Ban quản lý phê duyệt thành công. Hợp đồng điện tử đã được tạo trên hệ thống với các thông tin sau:
                        </p>

                        <div style="background-color: #f1f5f9; border-left: 4px solid #3b82f6; padding: 20px; margin: 25px 0; border-radius: 4px;">
                            <h3 style="margin-top: 0; color: #1e293b; font-size: 16px;">📋 CHI TIẾT HỢP ĐỒNG</h3>
                            <table style="width: 100%%; border-collapse: collapse; font-size: 14px;">
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b; width: 140px;"><strong>Phòng thuê:</strong></td>
                                    <td style="padding: 6px 0; color: #1e293b; font-weight: bold;">Phòng %s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b;"><strong>Ngày bắt đầu:</strong></td>
                                    <td style="padding: 6px 0; color: #1e293b; font-weight: bold;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b;"><strong>Ngày kết thúc:</strong></td>
                                    <td style="padding: 6px 0; color: #1e293b; font-weight: bold;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b;"><strong>Giá thuê hàng tháng:</strong></td>
                                    <td style="padding: 6px 0; color: #10b981; font-weight: bold;">%s VNĐ</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b;"><strong>Tiền cọc:</strong></td>
                                    <td style="padding: 6px 0; color: #f59e0b; font-weight: bold;">%s VNĐ</td>
                                </tr>
                            </table>
                        </div>
                        
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
                            <a href="https://quan-ly-phong-tro-frontend-6fx2h31g2.vercel.app/login" style="background-color: #0f766e; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 12px rgba(15,118,110,0.3);">Đăng nhập ngay</a>
                        </div>
                    </div>
                    
                    <div style="background-color: #e2e8f0; padding: 20px; text-align: center; color: #64748b; font-size: 12px;">
                        <p style="margin: 0;">© 2026 Smart Phòng Trọ. Tất cả các quyền được bảo lưu.</p>
                        <p style="margin: 5px 0 0 0;">Đây là email tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
                """.formatted(displayName, safeRoom, safeRoom, startDate, endDate, rentPrice, deposit, loginPhone, safePass);
    }

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void sendExistingUserContractEmail(com.example.quanliPT.model.Contract contract) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(contract.getTenant().getEmail());
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setSubject("Smart Phòng Trọ - Đơn thuê phòng đã được duyệt!");
            helper.setText(buildExistingUserContractEmailHtml(contract), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể tạo email thông báo duyệt", e);
        }
    }

    private String buildExistingUserContractEmailHtml(com.example.quanliPT.model.Contract contract) {
        String fullName = contract.getTenant().getFullName();
        String displayName = fullName == null || fullName.isBlank() ? "Quý khách" : HtmlUtils.htmlEscape(fullName);
        String safeRoom = HtmlUtils.htmlEscape(contract.getRoom().getRoomNumber());
        String rentPrice = formatCurrency(contract.getRentPrice());
        String deposit = formatCurrency(contract.getDeposit());
        String startDate = formatDate(contract.getStartDate());
        String endDate = formatDate(contract.getEndDate());
        
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f8fafc; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0;">
                    <div style="background: linear-gradient(135deg, #0f766e 0%%, #0d9488 100%%); padding: 30px 20px; text-align: center;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 0.5px;">SMART PHÒNG TRỌ</h1>
                        <p style="color: #ccfbf1; margin: 10px 0 0 0; font-size: 16px;">Thông báo duyệt hợp đồng</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <h2 style="color: #0f766e; margin-top: 0; font-size: 20px;">Chúc mừng %s! 🎉</h2>
                        <p style="color: #334155; font-size: 16px; line-height: 1.6;">
                            Đơn đăng ký thuê <strong>Phòng %s</strong> của bạn đã được Ban quản lý phê duyệt thành công. Hợp đồng điện tử đã được tạo trên hệ thống với các thông tin sau:
                        </p>
                        
                        <div style="background-color: #f1f5f9; border-left: 4px solid #3b82f6; padding: 20px; margin: 25px 0; border-radius: 4px;">
                            <h3 style="margin-top: 0; color: #1e293b; font-size: 16px;">📋 CHI TIẾT HỢP ĐỒNG</h3>
                            <table style="width: 100%%; border-collapse: collapse; font-size: 14px;">
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b; width: 140px;"><strong>Phòng thuê:</strong></td>
                                    <td style="padding: 6px 0; color: #1e293b; font-weight: bold;">Phòng %s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b;"><strong>Ngày bắt đầu:</strong></td>
                                    <td style="padding: 6px 0; color: #1e293b; font-weight: bold;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b;"><strong>Ngày kết thúc:</strong></td>
                                    <td style="padding: 6px 0; color: #1e293b; font-weight: bold;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b;"><strong>Giá thuê hàng tháng:</strong></td>
                                    <td style="padding: 6px 0; color: #10b981; font-weight: bold;">%s VNĐ</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b;"><strong>Tiền cọc:</strong></td>
                                    <td style="padding: 6px 0; color: #f59e0b; font-weight: bold;">%s VNĐ</td>
                                </tr>
                            </table>
                        </div>

                        <div style="background-color: #ffffff; border-left: 4px solid #0f766e; padding: 20px; margin: 25px 0; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
                            <h3 style="margin-top: 0; color: #1e293b; font-size: 16px;">THÔNG TIN ĐĂNG NHẬP</h3>
                            <p style="color: #64748b; margin-bottom: 0; font-size: 14px;">Bạn đã có sẵn tài khoản trên hệ thống. Vui lòng đăng nhập bằng <strong>số điện thoại</strong> và <strong>mật khẩu hiện tại</strong> của bạn để xem chi tiết hợp đồng và theo dõi hóa đơn hàng tháng.</p>
                        </div>
                        
                        <div style="text-align: center; margin-top: 35px;">
                            <a href="https://quan-ly-phong-tro-frontend-6fx2h31g2.vercel.app/login" style="background-color: #0f766e; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 12px rgba(15,118,110,0.3);">Đăng nhập ngay</a>
                        </div>
                    </div>
                    
                    <div style="background-color: #e2e8f0; padding: 20px; text-align: center; color: #64748b; font-size: 12px;">
                        <p style="margin: 0;">© 2026 Smart Phòng Trọ. Tất cả các quyền được bảo lưu.</p>
                        <p style="margin: 5px 0 0 0;">Đây là email tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
                """.formatted(displayName, safeRoom, safeRoom, startDate, endDate, rentPrice, deposit);
    }
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void sendInvoiceNotificationEmail(com.example.quanliPT.model.Invoice invoice) {
        try {
            if (invoice.getContract().getTenant() == null || invoice.getContract().getTenant().getEmail() == null) {
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(invoice.getContract().getTenant().getEmail());
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setSubject("Smart Phòng Trọ - Thông báo có Hóa đơn mới!");
            
            String fullName = HtmlUtils.htmlEscape(invoice.getContract().getTenant().getFullName());
            String roomNumber = HtmlUtils.htmlEscape(invoice.getContract().getRoom().getRoomNumber());
            String totalAmount = formatCurrency(invoice.getTotalAmount());
            String month = String.valueOf(invoice.getBillingDate().getMonthValue());
            String year = String.valueOf(invoice.getBillingDate().getYear());

            String html = """
                <div style="font-family:Arial,sans-serif;line-height:1.6;color:#222">
                    <h2>Thông báo Hóa Đơn Mới - Tháng %s/%s</h2>
                    <p>Xin chào %s,</p>
                    <p>Hệ thống vừa tạo hóa đơn định kỳ cho <strong>Phòng %s</strong> của bạn.</p>
                    <p>Tổng tiền thanh toán tạm tính: <strong style="color:red">%s VNĐ</strong></p>
                    <p>Vui lòng đăng nhập vào hệ thống để xem chi tiết số điện/nước và thanh toán.</p>
                    <p>Trân trọng,<br>Ban quản lý Smart Phòng Trọ</p>
                </div>
                """.formatted(month, year, fullName, roomNumber, totalAmount);
                
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể tạo email thông báo hóa đơn", e);
        }
    }
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void sendPaymentSuccessEmail(com.example.quanliPT.model.Invoice invoice) {
        try {
            if (invoice.getContract().getTenant() == null || invoice.getContract().getTenant().getEmail() == null) {
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(invoice.getContract().getTenant().getEmail());
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setSubject("Smart Phòng Trọ - BIÊN LAI XÁC NHẬN THANH TOÁN THÀNH CÔNG");
            
            String fullName = HtmlUtils.htmlEscape(invoice.getContract().getTenant().getFullName());
            String roomNumber = HtmlUtils.htmlEscape(invoice.getContract().getRoom().getRoomNumber());
            String totalAmount = formatCurrency(invoice.getTotalAmount());
            
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String paymentDate = now.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String paymentTime = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String invoiceId = "HD" + String.format("%06d", invoice.getId());

            String html = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #f8fafc; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px rgba(0,0,0,0.05);">
                    <div style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 25px 20px; text-align: center;">
                        <div style="background-color: white; width: 60px; height: 60px; border-radius: 50%%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;">
                            <span style="color: #10b981; font-size: 30px;">✓</span>
                        </div>
                        <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: bold;">THANH TOÁN THÀNH CÔNG</h1>
                        <p style="color: #d1fae5; margin: 8px 0 0 0; font-size: 15px;">Biên lai điện tử - Smart Phòng Trọ</p>
                    </div>
                    
                    <div style="padding: 30px;">
                        <p style="color: #334155; font-size: 16px;">Kính gửi <strong>%s</strong>,</p>
                        <p style="color: #334155; font-size: 15px; line-height: 1.6;">
                            Hệ thống đã ghi nhận khoản thanh toán của bạn cho <strong>Phòng %s</strong>. Dưới đây là thông tin chi tiết biên lai của bạn:
                        </p>
                        
                        <div style="background-color: #ffffff; border: 1px dashed #cbd5e1; padding: 20px; margin: 25px 0; border-radius: 8px;">
                            <table style="width: 100%%; border-collapse: collapse; font-size: 14px;">
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; width: 140px;"><strong>Mã hóa đơn:</strong></td>
                                    <td style="padding: 8px 0; color: #1e293b; font-weight: 600;">%s</td>
                                </tr>
                                <tr style="border-bottom: 1px solid #f1f5f9;">
                                    <td style="padding: 8px 0; color: #64748b;"><strong>Ngày giờ thu tiền:</strong></td>
                                    <td style="padding: 8px 0; color: #1e293b; font-weight: 600;">%s lúc %s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;"><strong>Kênh thanh toán:</strong></td>
                                    <td style="padding: 8px 0; color: #1e293b; font-weight: 600;">Chuyển khoản (PayOS)</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;"><strong>Trạng thái:</strong></td>
                                    <td style="padding: 8px 0; color: #10b981; font-weight: bold;">HOÀN TẤT</td>
                                </tr>
                                <tr><td colspan="2"><hr style="border: 0; border-top: 1px dashed #cbd5e1; margin: 10px 0;"></td></tr>
                                <tr>
                                    <td style="padding: 12px 0 0 0; color: #64748b; font-size: 16px;"><strong>TỔNG TIỀN:</strong></td>
                                    <td style="padding: 12px 0 0 0; color: #059669; font-weight: 800; font-size: 22px;">%s VNĐ</td>
                                </tr>
                            </table>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; font-style: italic; text-align: center;">
                            * Email này có giá trị thay thế cho biên lai thu tiền giấy.
                        </p>
                    </div>
                    
                    <div style="background-color: #f1f5f9; padding: 20px; text-align: center; color: #64748b; font-size: 13px; border-top: 1px solid #e2e8f0;">
                        <p style="margin: 0; font-weight: bold; color: #475569;">BQL SMART PHÒNG TRỌ</p>
                        <p style="margin: 5px 0 0 0;">Cảm ơn bạn đã thanh toán đúng hạn!</p>
                    </div>
                </div>
                """.formatted(fullName, roomNumber, invoiceId, paymentDate, paymentTime, totalAmount);
                
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể tạo email xác nhận thanh toán", e);
        }
    }
}

