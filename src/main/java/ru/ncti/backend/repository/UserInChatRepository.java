package ru.ncti.backend.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.UserInChat;

@Repository
public interface UserInChatRepository extends CrudRepository<UserInChat, String> {
}
