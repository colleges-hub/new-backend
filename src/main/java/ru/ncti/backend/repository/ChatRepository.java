package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.entity.Chat;
import ru.ncti.backend.entity.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {
    @Query("SELECT c FROM Chat c JOIN c.users u WHERE u = :user")
    List<Chat> findByUser(@Param("user") User user);

    @Query("SELECT c FROM Chat c JOIN c.users u WHERE c.id = :chat and u.id = :user")
    Chat findByIdAndUsersIn(UUID chat, Long user);
}
