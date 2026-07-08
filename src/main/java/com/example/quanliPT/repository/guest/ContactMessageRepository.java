package com.example.quanliPT.repository.guest;

import com.example.quanliPT.model.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}

