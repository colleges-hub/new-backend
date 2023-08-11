package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.Speciality;

/**
 * user: ichuvilin
 */
@Repository
public interface SpecialityRepository extends JpaRepository<Speciality, String> {
}
