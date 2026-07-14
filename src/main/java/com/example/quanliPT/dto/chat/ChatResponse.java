package com.example.quanliPT.dto.chat;

import com.example.quanliPT.model.Room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private List<Room> roomsInfo;

    public ChatResponse(String reply) {
        this.reply = reply;
    }
}

