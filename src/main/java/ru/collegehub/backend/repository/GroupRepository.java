package ru.collegehub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
}
