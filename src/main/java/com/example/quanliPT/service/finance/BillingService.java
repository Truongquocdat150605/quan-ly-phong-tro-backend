package com.example.quanliPT.service.finance;

import com.example.quanliPT.model.Contract;
import com.example.quanliPT.model.Invoice;
import com.example.quanliPT.model.RentalService;
import com.example.quanliPT.model.enums.ContractStatus;
import com.example.quanliPT.model.enums.InvoiceStatus;
import com.example.quanliPT.model.enums.ServiceCategory;
import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.repository.finance.InvoiceRepository;
import com.example.quanliPT.repository.room.RentalServiceRepository;
import com.example.quanliPT.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    public record BillingRunResult(int created, int skipped, int failed) {}

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final RentalServiceRepository rentalServiceRepository;
    private final EmailService emailService;

    @Transactional
    public BillingRunResult generateMonthlyInvoices() {
        return generateMonthlyInvoices(false);
    }

    @Transactional
    public BillingRunResult generateMonthlyInvoices(boolean forceCreate) {
        log.info("Starting automated monthly invoice generation...");
        List<Contract> activeContracts = contractRepository.findByActiveTrueAndStatus(ContractStatus.ACTIVE);
        log.debug("Found {} active contracts for monthly invoicing", activeContracts.size());
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (Contract contract : activeContracts) {
            try {
                if (generateInvoiceForContract(contract, forceCreate)) {
                    created++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.error("Failed to generate monthly invoice for contract id={}: {}", contract.getId(), e.getMessage(), e);
            }
        }

        log.info("Monthly invoice generation completed: created={}, skipped={}, failed={}", created, skipped, failed);
        return new BillingRunResult(created, skipped, failed);
    }

    @Transactional
    public boolean generateInvoiceForContract(Contract contract) {
        return generateInvoiceForContract(contract, false);
    }

    @Transactional
    public boolean generateInvoiceForContract(Contract contract, boolean forceCreate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        log.info("Generating invoice for Contract ID: {}", contract.getId());

        if (!forceCreate && invoiceRepository.existsByContractIdAndBillingDateBetween(
                contract.getId(), monthStart, nextMonthStart.minusNanos(1))) {
            log.info("Invoice for contract id={} already exists in {}/{}. Skipping.",
                    contract.getId(), now.getMonthValue(), now.getYear());
            return false;
        }

        BigDecimal roomServiceFees = Optional.ofNullable(contract.getRoom().getServices())
                .orElse(Set.of())
                .stream()
                .filter(RentalService::isActive)
                .filter(service -> service.getCategory() != ServiceCategory.ELECTRICITY
                        && service.getCategory() != ServiceCategory.WATER)
                .map(RentalService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal electricityPrice = rentalServiceRepository.findByCategory(ServiceCategory.ELECTRICITY)
                .stream()
                .findFirst()
                .map(RentalService::getPrice)
                .orElse(BigDecimal.valueOf(3500));

        BigDecimal waterPrice = rentalServiceRepository.findByCategory(ServiceCategory.WATER)
                .stream()
                .findFirst()
                .map(RentalService::getPrice)
                .orElse(BigDecimal.valueOf(15000));

        Invoice invoice = Invoice.builder()
                .contract(contract)
                .rentalAmount(contract.getRentPrice())
                .electricityStart(0.0)
                .electricityEnd(0.0)
                .electricityPrice(electricityPrice)
                .electricityAmount(BigDecimal.ZERO)
                .waterStart(0.0)
                .waterEnd(0.0)
                .waterPrice(waterPrice)
                .waterAmount(BigDecimal.ZERO)
                .serviceAmount(roomServiceFees)
                .totalAmount(contract.getRentPrice().add(roomServiceFees))
                .billingDate(now)
                .status(InvoiceStatus.UNPAID)
                .notes((forceCreate ? "Hoa don demo thang " : "Hoa don thang ") + now.getMonthValue())
                .build();

        invoiceRepository.save(invoice);
        log.info("Invoice saved for contract id={} with totalAmount={}", contract.getId(), invoice.getTotalAmount());

        try {
            emailService.sendInvoiceNotificationEmail(invoice);
            log.info("Sent email notification for new invoice to tenant {}", contract.getTenant().getEmail());
        } catch (Exception e) {
            log.error("Failed to send email notification for new invoice: {}", e.getMessage(), e);
        }
        return true;
    }
}
