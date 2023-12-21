package ru.collegehub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Subject;

@Repository
public interface StudentRepository extends JpaRepository<Subject, Long> {
}
