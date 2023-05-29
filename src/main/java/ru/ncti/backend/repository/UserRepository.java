package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.entity.Group;
import ru.ncti.backend.entity.Role;
import ru.ncti.backend.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByLastnameAndFirstname(String lastname, String firstname);

    List<User> findAllByGroupOrderByLastname(Group group);

    List<User> findAllByRoles(Role role);
}
