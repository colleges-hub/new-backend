package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.PublicChat;
import ru.ncti.backend.model.User;

import java.util.List;
import java.util.UUID;

/**
 * user: ichuvilin
 */
@Repository
public interface PublicChatRepository extends JpaRepository<PublicChat, UUID> {

    List<PublicChat> findByUsers(@Param("user") User user);

    @Query("SELECT c FROM PublicChat c JOIN c.users u WHERE c.id = :chat and u.id = :user")
    PublicChat findByIdAndUsersIn(UUID chat, Long user);
}
