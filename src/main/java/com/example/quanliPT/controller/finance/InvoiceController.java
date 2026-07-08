package com.example.quanliPT.controller.finance;

import com.example.quanliPT.model.Invoice;
import com.example.quanliPT.model.enums.InvoiceStatus;
import com.example.quanliPT.repository.finance.InvoiceRepository;
import com.example.quanliPT.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

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
        invoice.setTotalAmount(invoiceDetails.getTotalAmount());
        invoice.setStatus(invoiceDetails.getStatus());
        invoice.setNotes(invoiceDetails.getNotes());
        if(invoice.getStatus() == InvoiceStatus.PAID && invoice.getPaymentDate() == null) {
            invoice.setPaymentDate(LocalDateTime.now());
        }
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}


