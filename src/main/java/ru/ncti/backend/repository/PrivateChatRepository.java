package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.PrivateChat;
import ru.ncti.backend.model.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrivateChatRepository extends JpaRepository<PrivateChat, UUID> {
    List<PrivateChat> findAllByUser1OrUser2(User user1, User user2);

    PrivateChat findByUser1AndUser2OrUser1AndUser2(User user1, User user2, User user2Again, User user1Again);


    PrivateChat findByIdAndUser1AndUser2OrUser1AndUser2(UUID chat, User user1, User user2, User user2Again, User user1Again);

}
