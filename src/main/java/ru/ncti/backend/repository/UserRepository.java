package ru.ncti.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.Role;
import ru.ncti.backend.model.User;

import java.util.List;
import java.util.Optional;

/**
 * user: ichuvilin
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByLastnameAndFirstname(String lastname, String firstname);

    List<User> findAllByGroupOrderByLastname(Group group);

    List<User> findAllByRolesOrderByLastname(Role role);

    Page<User> findAllByOrderByLastname(Pageable pageable);
}
