package com.example.quanliPT.service.finance;

import com.example.quanliPT.model.*;
import com.example.quanliPT.model.enums.ContractStatus;
import com.example.quanliPT.model.enums.InvoiceStatus;
import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;

import com.example.quanliPT.model.Contract;
import com.example.quanliPT.model.Room;


import com.example.quanliPT.model.enums.ServiceCategory;
import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.repository.finance.InvoiceRepository;
import com.example.quanliPT.repository.room.RentalServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final RentalServiceRepository rentalServiceRepository;

    @Transactional
    public void generateMonthlyInvoices() {
        log.info("Starting automated monthly invoice generation...");
        List<Contract> activeContracts = contractRepository.findByActiveTrueAndStatus(ContractStatus.ACTIVE);
        log.debug("Found {} active contracts for monthly invoicing", activeContracts.size());
        for (Contract contract : activeContracts) {
            try {
                generateInvoiceForContract(contract);
            } catch (Exception e) {
                log.error("Failed to generate monthly invoice for contract id={}: {}", contract.getId(), e.getMessage(), e);
            }
        }
        log.info("Monthly invoice generation completed");
    }

    @Transactional
    public void generateInvoiceForContract(Contract contract) {
        log.info("Generating invoice for Contract ID: {}", contract.getId());
        log.debug("Contract details: rentPrice={}, servicesCount={}",
                contract.getRentPrice(),
                contract.getRoom().getServices() != null ? contract.getRoom().getServices().size() : 0);

        BigDecimal roomServiceFees = contract.getRoom().getServices().stream()
                .map(RentalService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("Total service fees calculated: {}", roomServiceFees);

        // Fetch dynamic electricity and water prices from RentalService table
        BigDecimal electricityPrice = rentalServiceRepository.findByCategory(ServiceCategory.ELECTRICITY)
                .stream().findFirst().map(RentalService::getPrice).orElse(BigDecimal.valueOf(3500));
        
        BigDecimal waterPrice = rentalServiceRepository.findByCategory(ServiceCategory.WATER)
                .stream().findFirst().map(RentalService::getPrice).orElse(BigDecimal.valueOf(15000));

        Invoice invoice = Invoice.builder()
                .contract(contract)
                .rentalAmount(contract.getRentPrice())
                .electricityStart(0.0)
                .electricityEnd(0.0)
                .electricityPrice(electricityPrice)
                .waterStart(0.0)
                .waterEnd(0.0)
                .waterPrice(waterPrice)
                .serviceAmount(roomServiceFees)
                .totalAmount(contract.getRentPrice().add(roomServiceFees))
                .billingDate(LocalDateTime.now())
                .status(InvoiceStatus.UNPAID)
                .notes("Hóa đơn tháng " + LocalDateTime.now().getMonthValue())
                .build();

        invoiceRepository.save(invoice);
        log.info("Invoice saved for contract id={} with totalAmount={}", contract.getId(), invoice.getTotalAmount());
    }
}





