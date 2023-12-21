package ru.collegehub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
