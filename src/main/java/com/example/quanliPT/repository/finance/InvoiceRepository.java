package com.example.quanliPT.repository.finance;

import com.example.quanliPT.model.Invoice;
import com.example.quanliPT.model.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Override
    @EntityGraph(attributePaths = {"contract", "contract.tenant", "contract.room"})
    List<Invoice> findAll();

    @EntityGraph(attributePaths = {"contract", "contract.tenant", "contract.room"})
    List<Invoice> findByContractTenantId(Long tenantId);

    long countByStatus(InvoiceStatus status);

    @EntityGraph(attributePaths = {"contract", "contract.tenant", "contract.room"})
    List<Invoice> findByStatus(InvoiceStatus status);

    @EntityGraph(attributePaths = {"contract", "contract.tenant", "contract.room"})
    List<Invoice> findByBillingDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") InvoiceStatus status);

    long countByStatusAndBillingDateBetween(
            InvoiceStatus status, LocalDateTime start, LocalDateTime end);

    boolean existsByContractIdAndStatus(Long contractId, InvoiceStatus status);
    boolean existsByContractIdAndBillingDateBetween(Long contractId, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"contract", "contract.tenant", "contract.room"})
    List<Invoice> findByContractId(Long contractId);
}
