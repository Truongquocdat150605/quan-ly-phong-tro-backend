package com.example.quanliPT.repository.finance;

import com.example.quanliPT.model.PaymentTransaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    @EntityGraph(attributePaths = {"invoice"})
    List<PaymentTransaction> findByInvoiceContractTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"invoice"})
    List<PaymentTransaction> findByInvoiceId(Long invoiceId);
}
