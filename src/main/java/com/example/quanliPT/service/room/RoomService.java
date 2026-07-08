package com.example.quanliPT.service.room;

import com.example.quanliPT.model.*;
import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;

import com.example.quanliPT.model.Room;

import com.example.quanliPT.repository.room.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    // 5 phòng mới nhất
    public List<Room> getNewestRooms() {
        Pageable limit5ByIdDesc = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));
        return roomRepository.findAvailableRooms(limit5ByIdDesc);
    }

    // 5 phòng trống được ưu tiên theo diện tích lớn hoặc loại phòng (hot nhất)
    public List<Room> getHotRooms() {
        // Ưu tiên: diện tích lớn trước, rồi đến type để ổn định
        Pageable limit5ByAreaDescThenTypeDesc = PageRequest.of(
                0,
                5,
                Sort.by(Sort.Direction.DESC, "area")
                        .and(Sort.by(Sort.Direction.DESC, "type"))
        );
        return roomRepository.findAvailableRooms(limit5ByAreaDescThenTypeDesc);
    }
}





