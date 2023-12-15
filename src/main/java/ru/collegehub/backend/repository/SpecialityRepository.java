package ru.collegehub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Speciality;


@Repository
public interface SpecialityRepository extends JpaRepository<Speciality, String> {
}
