package com.example.quanliPT.repository.guest;

import com.example.quanliPT.model.RentalRequest;
import com.example.quanliPT.model.enums.RentalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalRequestRepository extends JpaRepository<RentalRequest, Long> {
    List<RentalRequest> findByStatus(RentalRequestStatus status);
}

