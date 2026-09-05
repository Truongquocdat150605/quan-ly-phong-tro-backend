package com.example.quanliPT.repository.guest;

import com.example.quanliPT.model.RentalRequest;
import com.example.quanliPT.model.enums.RentalRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalRequestRepository extends JpaRepository<RentalRequest, Long> {
    @Override
    @EntityGraph(attributePaths = {"room"})
    List<RentalRequest> findAll();

    @EntityGraph(attributePaths = {"room"})
    List<RentalRequest> findTop5ByOrderByIdDesc();

    @EntityGraph(attributePaths = {"room"})
    List<RentalRequest> findByStatus(RentalRequestStatus status);

    List<RentalRequest> findByPhone(String phone);
}
