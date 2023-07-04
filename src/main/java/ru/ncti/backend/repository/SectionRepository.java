package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.Section;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
}
