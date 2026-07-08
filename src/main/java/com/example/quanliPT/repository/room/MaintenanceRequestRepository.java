package com.example.quanliPT.repository.room;

import com.example.quanliPT.model.MaintenanceRequest;
import com.example.quanliPT.model.Room;
import com.example.quanliPT.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByRoom(Room room);
    List<MaintenanceRequest> findByTenant(User tenant);
}

