package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.entity.Chat;
import ru.ncti.backend.entity.ChatMessage;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findAllByChatOrderByCreatedAtDesc(Chat chat);
}
