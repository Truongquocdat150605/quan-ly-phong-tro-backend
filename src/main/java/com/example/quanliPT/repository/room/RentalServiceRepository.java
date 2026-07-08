package com.example.quanliPT.repository.room;

import com.example.quanliPT.model.RentalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.example.quanliPT.model.enums.ServiceCategory;

@Repository
public interface RentalServiceRepository extends JpaRepository<RentalService, Long> {
    List<RentalService> findByCategory(ServiceCategory category);
}

