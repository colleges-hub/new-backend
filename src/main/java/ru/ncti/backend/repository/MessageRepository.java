package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.entity.Chat;
import ru.ncti.backend.entity.Message;

import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findAllByChatOrderByCreatedAtDesc(Chat chat);
}
