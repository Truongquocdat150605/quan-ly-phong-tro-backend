package com.example.quanliPT.controller.finance;

import com.example.quanliPT.model.Invoice;
import com.example.quanliPT.model.enums.InvoiceStatus;
import com.example.quanliPT.repository.finance.InvoiceRepository;
import com.example.quanliPT.repository.user.UserRepository;
import com.example.quanliPT.service.task.BillingScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final com.example.quanliPT.repository.finance.PaymentTransactionRepository paymentTransactionRepository;
    private final com.example.quanliPT.service.finance.BillingService billingService;
    private final BillingScheduler billingScheduler;

    @PostMapping("/generate-monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> generateMonthlyInvoices(
            @RequestParam(defaultValue = "false") boolean force) {
        billingService.generateMonthlyInvoices(force);
        return ResponseEntity.ok("Successfully generated monthly invoices.");
    }

    @GetMapping("/scheduler-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        return ResponseEntity.ok(billingScheduler.getStatus());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @GetMapping("/my-invoices/{tenantId}")
    @PreAuthorize("@invoiceSecurity.canAccessTenantInvoices(#tenantId, authentication)")
    public List<Invoice> getMyInvoices(@PathVariable Long tenantId) {
        return invoiceRepository.findByContractTenantId(tenantId);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    public List<Invoice> getCurrentTenantInvoices(Authentication authentication) {
        var user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return invoiceRepository.findByContractTenantId(user.getId());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        if(invoice.getBillingDate() == null) invoice.setBillingDate(LocalDateTime.now());
        if(invoice.getStatus() == null) invoice.setStatus(InvoiceStatus.UNPAID);
        recalculateInvoiceAmounts(invoice);
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Invoice> updateInvoice(@PathVariable Long id, @RequestBody Invoice invoiceDetails) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow();
        invoice.setRentalAmount(invoiceDetails.getRentalAmount());
        invoice.setElectricityStart(invoiceDetails.getElectricityStart());
        invoice.setElectricityEnd(invoiceDetails.getElectricityEnd());
        invoice.setElectricityPrice(invoiceDetails.getElectricityPrice());
        invoice.setElectricityAmount(invoiceDetails.getElectricityAmount());
        invoice.setWaterStart(invoiceDetails.getWaterStart());
        invoice.setWaterEnd(invoiceDetails.getWaterEnd());
        invoice.setWaterPrice(invoiceDetails.getWaterPrice());
        invoice.setWaterAmount(invoiceDetails.getWaterAmount());
        invoice.setServiceAmount(invoiceDetails.getServiceAmount());
        invoice.setStatus(invoiceDetails.getStatus());
        invoice.setNotes(invoiceDetails.getNotes());
        recalculateInvoiceAmounts(invoice);
        if(invoice.getStatus() == InvoiceStatus.PAID && invoice.getPaymentDate() == null) {
            invoice.setPaymentDate(LocalDateTime.now());
        }
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    private void recalculateInvoiceAmounts(Invoice invoice) {
        BigDecimal rentalAmount = defaultMoney(invoice.getRentalAmount());
        BigDecimal electricityPrice = defaultMoney(invoice.getElectricityPrice());
        BigDecimal waterPrice = defaultMoney(invoice.getWaterPrice());
        BigDecimal serviceAmount = defaultMoney(invoice.getServiceAmount());

        double electricityUsage = Math.max(0, defaultNumber(invoice.getElectricityEnd()) - defaultNumber(invoice.getElectricityStart()));
        double waterUsage = Math.max(0, defaultNumber(invoice.getWaterEnd()) - defaultNumber(invoice.getWaterStart()));

        BigDecimal electricityAmount = BigDecimal.valueOf(electricityUsage).multiply(electricityPrice);
        BigDecimal waterAmount = BigDecimal.valueOf(waterUsage).multiply(waterPrice);

        invoice.setRentalAmount(rentalAmount);
        invoice.setElectricityPrice(electricityPrice);
        invoice.setElectricityAmount(electricityAmount);
        invoice.setWaterPrice(waterPrice);
        invoice.setWaterAmount(waterAmount);
        invoice.setServiceAmount(serviceAmount);
        invoice.setTotalAmount(rentalAmount.add(electricityAmount).add(waterAmount).add(serviceAmount));
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double defaultNumber(Double value) {
        return value != null ? value : 0.0;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        List<com.example.quanliPT.model.PaymentTransaction> transactions = paymentTransactionRepository.findByInvoiceId(id);
        if (transactions != null && !transactions.isEmpty()) {
            paymentTransactionRepository.deleteAll(transactions);
        }
        invoiceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}


