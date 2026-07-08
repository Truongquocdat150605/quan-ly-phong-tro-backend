package com.example.quanliPT.repository.contract;

import com.example.quanliPT.model.Contract;
import com.example.quanliPT.model.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"tenant", "room"})
    List<Contract> findByTenantId(Long tenantId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"tenant", "room"})
    List<Contract> findByTenantIdAndActiveTrueAndStatus(Long tenantId, ContractStatus status);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"tenant", "room"})
    List<Contract> findByRoomIdAndActiveTrue(Long roomId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"tenant", "room"})
    List<Contract> findByActiveTrueAndStatus(ContractStatus status);
    long countByStatus(ContractStatus status);
}

