package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.Message;
import ru.ncti.backend.model.PrivateChat;
import ru.ncti.backend.model.PublicChat;

import java.util.List;
import java.util.UUID;

/**
 * user: ichuvilin
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findAllByPublicChatOrderByCreatedAtAsc(PublicChat publicChat);

    List<Message> findAllByPrivateChatOrderByCreatedAtAsc(PrivateChat chat);
}
