package ru.ncti.backend.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.ChatData;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ChatDataRepository extends CrudRepository<ChatData, String> {
    Optional<ChatData> findByUsername(String username);

    Set<ChatData> findAllByChat(String chat);
}
