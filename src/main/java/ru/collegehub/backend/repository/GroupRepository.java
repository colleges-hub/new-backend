package ru.collegehub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Group;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query(value = "SELECT g FROM Group g JOIN FETCH  g.speciality where g.id = :id")
    Optional<Group> findByIdFetchSpeciality(Long id);
    Optional<Group> findByNameIgnoreCase(String name);
}
