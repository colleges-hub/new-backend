package ru.collegehub.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Group;
import ru.collegehub.backend.model.Role;
import ru.collegehub.backend.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmail(String email);

    Optional<User> findByLastnameAndFirstname(String lastname, String firstname);

    List<User> findAllByGroupOrderByLastname(Group group);
    
    List<User> findAllByRolesOrderByLastname(Role role);

    Page<User> findAllByOrderByLastname(Pageable pageable);
}
