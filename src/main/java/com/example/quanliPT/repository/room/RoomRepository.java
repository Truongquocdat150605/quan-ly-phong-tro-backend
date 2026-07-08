package com.example.quanliPT.repository.room;

import com.example.quanliPT.model.Room;
import com.example.quanliPT.model.enums.RoomStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"services"})
    List<Room> findByStatus(RoomStatus status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"services"})
    List<Room> findByStatus(RoomStatus status, Pageable pageable);

    default List<Room> findAvailableRooms(Pageable pageable) {
        return findByStatus(RoomStatus.AVAILABLE, pageable);
    }

    long countByStatus(RoomStatus status);
}


