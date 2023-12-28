package ru.collegehub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Role;
import ru.collegehub.backend.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = "SELECT u FROM User u JOIN FETCH u.roles r where u.email = :email")
    Optional<User> findByEmail(String email);

    @Query(value = "SELECT u FROM User u JOIN FETCH u.roles r JOIN r.role")
    List<User> findAllByQuery();

    @Query(value = "SELECT u FROM User u JOIN FETCH u.roles r JOIN r.role rr where rr = :role")
    List<User> findAllByRole(Role role);
}
