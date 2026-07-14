package com.example.quanliPT.controller.finance;

import org.springframework.http.MediaType;

import com.example.quanliPT.model.*;
import com.example.quanliPT.model.enums.InvoiceStatus;
import com.example.quanliPT.model.enums.PaymentMethod;
import com.example.quanliPT.model.enums.PaymentStatus;
import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;

import com.example.quanliPT.repository.user.UserRepository;
import com.example.quanliPT.model.PaymentTransaction;
import com.example.quanliPT.model.Contract;


import com.example.quanliPT.repository.finance.InvoiceRepository;
import com.example.quanliPT.repository.finance.PaymentTransactionRepository;

import com.example.quanliPT.config.PayOSConfig;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;
    private final com.example.quanliPT.service.notification.EmailService emailService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Autowired
    private PayOSConfig payOSConfig;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean canAccessInvoice(Invoice invoice, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return invoice != null && invoice.getContract() != null && invoice.getContract().getTenant() != null
                && invoice.getContract().getTenant().getId().equals(user.getId());
    }

    private void cancelPendingPayments(Long invoiceId, PaymentMethod exceptMethod) {
        List<PaymentTransaction> existingTxs = paymentRepository.findByInvoiceId(invoiceId);
        for (PaymentTransaction existingTx : existingTxs) {
            if (existingTx.getStatus() == PaymentStatus.PENDING && existingTx.getMethod() != exceptMethod) {
                existingTx.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(existingTx);
            }
        }
    }

    private PaymentTransaction findPendingCashPayment(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .filter(tx -> tx.getMethod() == PaymentMethod.CASH && tx.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    // Helper: Tạo chữ ký HMAC SHA256 cho PayOS
    private String hmacSHA256(String data, String key) {
        try {
            javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(),
                    "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chữ ký HMAC SHA256", e);
        }
    }

    @PostMapping("/stripe/{invoiceId}")
    @PreAuthorize("hasAnyRole('TENANT','ADMIN')")
    public ResponseEntity<?> payWithStripe(@PathVariable Long invoiceId, Authentication authentication) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn!"));

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Hóa đơn này đã được thanh toán rồi!"));
            }

            // Hủy các giao dịch PENDING cũ của hóa đơn này để tránh trùng lặp
            List<PaymentTransaction> existingTxs = paymentRepository.findByInvoiceId(invoiceId);
            for (PaymentTransaction existingTx : existingTxs) {
                if (existingTx.getStatus() == PaymentStatus.PENDING) {
                    existingTx.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(existingTx);
                }
            }

            // 1. Tạo giao dịch PENDING trước để lấy ID
            PaymentTransaction tx = PaymentTransaction.builder()
                    .invoice(invoice)
                    .method(PaymentMethod.STRIPE)
                    .status(PaymentStatus.PENDING)
                    .amount(invoice.getTotalAmount())
                    .notes("Đang khởi tạo giao dịch Stripe")
                    .build();
            tx = paymentRepository.save(tx);

            long stripeAmount = invoice.getTotalAmount().longValue();
            // Stripe khong ho tro VND. Quy doi: 1 USD = 25,000 VND, Stripe tinh theo cents (x100)
            // Vi du: 2,500,000 VND -> 2,500,000 / 250 = 10,000 cents = $100 USD
            long amountInCents = stripeAmount / 250;
            if (amountInCents < 50) amountInCents = 50; // Stripe yeu cau toi thieu $0.50

            // 2. Tạo Session Stripe Checkout
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:3000/my-invoices?status=PAID&invoiceId=" + invoiceId + "&paymentId=" + tx.getId())
                    .setCancelUrl("http://localhost:3000/my-invoices?status=CANCELED&invoiceId=" + invoiceId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd") // Stripe khong ho tro VND
                                                    .setUnitAmount(amountInCents) // Da quy doi sang USD cents
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Thanh toan hoa don phong tro #" + invoiceId
                                                                        + " (" + String.format("%,d", stripeAmount) + " VND)")
                                                                    .build())
                                                    .build())
                                    .build())
                    .putMetadata("invoiceId", invoiceId.toString())
                    .putMetadata("paymentId", tx.getId().toString())
                    .build();

            Session session = Session.create(params);

            // 3. Cập nhật lại thông tin giao dịch
            tx.setTransactionCode(session.getId());
            tx.setQrUrl(session.getUrl()); // lưu checkout URL vào trường qrUrl để frontend đọc
            tx.setNotes("Thanh toán qua Stripe Checkout");
            paymentRepository.save(tx);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "payUrl", session.getUrl(),
                    "paymentId", tx.getId()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi tạo thanh toán Stripe: " + e.getMessage()));
        }
    }

    @PostMapping("/payos/{invoiceId}")
    @PreAuthorize("hasAnyRole('TENANT','ADMIN')")
    public ResponseEntity<?> payWithPayOS(@PathVariable Long invoiceId, Authentication authentication) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn!"));

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Hóa đơn này đã được thanh toán rồi!"));
            }

            // Hủy các giao dịch PENDING cũ của hóa đơn này để tránh trùng lặp
            List<PaymentTransaction> existingTxs = paymentRepository.findByInvoiceId(invoiceId);
            for (PaymentTransaction existingTx : existingTxs) {
                if (existingTx.getStatus() == PaymentStatus.PENDING) {
                    existingTx.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(existingTx);
                }
            }

            // 1. Tạo giao dịch PENDING trước để lấy ID duy nhất làm orderCode cho PayOS
            PaymentTransaction tx = PaymentTransaction.builder()
                    .invoice(invoice)
                    .method(PaymentMethod.PAYOS)
                    .status(PaymentStatus.PENDING)
                    .amount(invoice.getTotalAmount())
                    .notes("Đang khởi tạo giao dịch PayOS")
                    .build();
            tx = paymentRepository.save(tx);

            long orderCode = tx.getId();
            int amount = invoice.getTotalAmount().intValue();

            // Chuẩn hóa mô tả thanh toán (Không dấu, không ký tự đặc biệt, độ dài tối đa 25 ký tự)
            String desc = "Thanh toan HD " + invoiceId;
            if (desc.length() > 25) {
                desc = desc.substring(0, 25);
            }

            String returnUrl = "http://localhost:3000/my-invoices?status=PAID&invoiceId=" + invoiceId + "&paymentId=" + tx.getId();
            String cancelUrl = "http://localhost:3000/my-invoices?status=CANCELED&invoiceId=" + invoiceId;

            // Xây dựng body cho request PayOS
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderCode", orderCode);
            requestBody.put("amount", amount);
            requestBody.put("description", desc);
            requestBody.put("returnUrl", returnUrl);
            requestBody.put("cancelUrl", cancelUrl);

            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("name", "Hoa don phong tro #" + invoiceId);
            item.put("quantity", 1);
            item.put("price", amount);
            items.add(item);
            requestBody.put("items", items);

            // Tạo signature
            String dataForSignature = "amount=" + amount +
                    "&cancelUrl=" + cancelUrl +
                    "&description=" + desc +
                    "&orderCode=" + orderCode +
                    "&returnUrl=" + returnUrl;

            String signature = hmacSHA256(dataForSignature, payOSConfig.getChecksumKey());
            requestBody.put("signature", signature);

            // Gửi POST request tới API PayOS
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", payOSConfig.getClientId());
            headers.set("x-api-key", payOSConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api-merchant.payos.vn/v2/payment-requests", entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if ("00".equals(responseBody.get("code"))) {
                    Map<String, Object> responseData = (Map<String, Object>) responseBody.get("data");
                    String checkoutUrl = (String) responseData.get("checkoutUrl");

                    // Cập nhật lại giao dịch với mã code và checkout URL
                    tx.setTransactionCode(String.valueOf(orderCode));
                    tx.setQrUrl(checkoutUrl);
                    tx.setNotes("Thanh toán qua PayOS");
                    paymentRepository.save(tx);

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "payUrl", checkoutUrl,
                            "paymentId", tx.getId()));
                } else {
                    throw new RuntimeException("PayOS trả về lỗi: " + responseBody.get("desc"));
                }
            } else {
                throw new RuntimeException("Lỗi gọi API PayOS");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi tạo thanh toán PayOS: " + e.getMessage()));
        }
    }

    @PostMapping("/cash/{invoiceId}")
    @PreAuthorize("hasAnyRole('TENANT','ADMIN')")
    public ResponseEntity<?> requestCashPayment(@PathVariable Long invoiceId, Authentication authentication) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Khong tim thay hoa don!"));

            if (!canAccessInvoice(invoice, authentication)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Ban khong co quyen thanh toan hoa don nay"));
            }

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Hoa don nay da duoc thanh toan roi!"));
            }

            cancelPendingPayments(invoiceId, PaymentMethod.CASH);

            PaymentTransaction tx = findPendingCashPayment(invoiceId);
            if (tx == null) {
                tx = PaymentTransaction.builder()
                        .invoice(invoice)
                        .method(PaymentMethod.CASH)
                        .status(PaymentStatus.PENDING)
                        .amount(invoice.getTotalAmount())
                        .transactionCode("CASH-PENDING-" + invoiceId)
                        .notes("Khach thue chon thanh toan tien mat, cho admin xac nhan da thu tien")
                        .build();
                tx = paymentRepository.save(tx);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentId", tx.getId(),
                    "message", "Da ghi nhan yeu cau thanh toan tien mat. Vui long cho admin xac nhan."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Loi ghi nhan thanh toan tien mat: " + e.getMessage()));
        }
    }

    @PutMapping("/cash/{invoiceId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> confirmCashPayment(@PathVariable Long invoiceId) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Khong tim thay hoa don!"));

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Hoa don nay da duoc thanh toan roi!"));
            }

            cancelPendingPayments(invoiceId, PaymentMethod.CASH);

            PaymentTransaction tx = findPendingCashPayment(invoiceId);
            if (tx == null) {
                tx = PaymentTransaction.builder()
                        .invoice(invoice)
                        .method(PaymentMethod.CASH)
                        .amount(invoice.getTotalAmount())
                        .build();
            }

            tx.setStatus(PaymentStatus.COMPLETED);
            tx.setAmount(invoice.getTotalAmount());
            tx.setTransactionCode("CASH-" + invoiceId + "-" + System.currentTimeMillis());
            tx.setNotes("Admin xac nhan da thu tien mat");
            paymentRepository.save(tx);

            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentDate(LocalDateTime.now());
            invoiceRepository.save(invoice);

            try {
                emailService.sendPaymentSuccessEmail(invoice);
            } catch (Exception e) {
                log.error("Failed to send cash payment success email: {}", e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentId", tx.getId(),
                    "message", "Da xac nhan thu tien mat va cap nhat hoa don thanh da thanh toan"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Loi xac nhan tien mat: " + e.getMessage()));
        }
    }

    @PutMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyRole('TENANT','ADMIN')")
    public ResponseEntity<?> confirmPayment(@PathVariable Long paymentId, Authentication authentication) {
        PaymentTransaction tx = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Chặn xác nhận lại giao dịch đã hoàn tất
        if (tx.getStatus() == PaymentStatus.COMPLETED) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Giao dịch này đã được xác nhận rồi!"));
        }

        // Chặn xác nhận hóa đơn đã được thanh toán
        Invoice invoice = tx.getInvoice();
        if (invoice != null && invoice.getStatus() == InvoiceStatus.PAID) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Hóa đơn này đã được thanh toán rồi!"));
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (invoice == null || invoice.getContract() == null || invoice.getContract().getTenant() == null
                    || !invoice.getContract().getTenant().getId().equals(user.getId())) {
                throw new RuntimeException("Bạn không có quyền xác nhận thanh toán này");
            }
        }

        tx.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(tx);

        invoice.setStatus(InvoiceStatus.PAID);
        if (invoice.getPaymentDate() == null) {
            invoice.setPaymentDate(LocalDateTime.now());
        }
        invoiceRepository.save(invoice);
        
        try {
            emailService.sendPaymentSuccessEmail(invoice);
        } catch (Exception e) {
            log.error("Failed to send payment success email: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Xác nhận thanh toán thành công"));
    }

    /**
     * Webhook tự động từ PayOS - Gọi khi thanh toán thành công.
     * PayOS gọi endpoint này với dữ liệu giao dịch, không cần đăng nhập.
     */
    @PostMapping("/payos/callback")
    public ResponseEntity<?> payosWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("[PayOS Webhook] Received payload: {}", payload);

            // 1. Lấy data từ payload
            Object dataObj = payload.get("data");
            Object signatureObj = payload.get("signature");
            if (dataObj == null || signatureObj == null) {
                log.warn("[PayOS Webhook] Missing data or signature");
                return ResponseEntity.badRequest().body(Map.of("error", "Missing data or signature"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            String receivedSignature = signatureObj.toString();

            // 2. Xác minh chữ ký HMAC (bảo mật - chặn giả mạo)
            // PayOS yêu cầu sắp xếp key theo thứ tự alphabet
            String orderCode = String.valueOf(data.getOrDefault("orderCode", ""));
            
            java.util.TreeMap<String, Object> sortedData = new java.util.TreeMap<>(data);
            StringBuilder dataToVerifyBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
                if (entry.getValue() != null && !String.valueOf(entry.getValue()).isEmpty()) {
                    if (dataToVerifyBuilder.length() > 0) {
                        dataToVerifyBuilder.append("&");
                    }
                    dataToVerifyBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                }
            }
            String dataToVerify = dataToVerifyBuilder.toString();

            String expectedSignature = hmacSHA256(dataToVerify, payOSConfig.getChecksumKey());
            if (!expectedSignature.equals(receivedSignature)) {
                log.warn("[PayOS Webhook] Invalid signature! Expected: {}, Got: {}", expectedSignature, receivedSignature);
                return ResponseEntity.status(400).body(Map.of("error", "Invalid signature"));
            }

            // 3. Kiểm tra trạng thái thanh toán
            String status = String.valueOf(data.getOrDefault("status", ""));
            if (!"PAID".equals(status)) {
                log.info("[PayOS Webhook] Payment not completed yet, status={}", status);
                return ResponseEntity.ok(Map.of("success", true, "message", "Acknowledged, not paid yet"));
            }

            // 4. Tìm giao dịch theo orderCode (= payment transaction ID)
            long txId;
            try {
                txId = Long.parseLong(orderCode);
            } catch (NumberFormatException e) {
                log.error("[PayOS Webhook] Invalid orderCode: {}", orderCode);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid orderCode"));
            }

            PaymentTransaction tx = paymentRepository.findById(txId).orElse(null);
            if (tx == null) {
                log.warn("[PayOS Webhook] PaymentTransaction not found for orderCode={}", txId);
                return ResponseEntity.ok(Map.of("success", true, "message", "Transaction not found, ignored"));
            }

            // 5. Cập nhật trạng thái nếu chưa COMPLETED
            if (tx.getStatus() != PaymentStatus.COMPLETED) {
                tx.setStatus(PaymentStatus.COMPLETED);
                tx.setNotes("Thanh toán qua PayOS - Xác nhận tự động");
                paymentRepository.save(tx);

                Invoice invoice = tx.getInvoice();
                if (invoice != null && invoice.getStatus() != InvoiceStatus.PAID) {
                    invoice.setStatus(InvoiceStatus.PAID);
                    invoice.setPaymentDate(LocalDateTime.now());
                    invoiceRepository.save(invoice);
                    log.info("[PayOS Webhook] Invoice id={} marked as PAID automatically!", invoice.getId());
                    
                    try {
                        emailService.sendPaymentSuccessEmail(invoice);
                    } catch (Exception e) {
                        log.error("[PayOS Webhook] Failed to send payment success email: {}", e.getMessage());
                    }
                }
            } else {
                log.info("[PayOS Webhook] Transaction id={} already COMPLETED, skipping", txId);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook processed successfully"));

        } catch (Exception e) {
            log.error("[PayOS Webhook] Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<PaymentTransaction>> getMyPayments(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(paymentRepository.findByInvoiceContractTenantId(user.getId()));
    }

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("@invoiceSecurity.canAccessInvoice(#invoiceId, authentication)")
    public ResponseEntity<List<PaymentTransaction>> getPaymentsByInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(paymentRepository.findByInvoiceId(invoiceId));
    }
}






