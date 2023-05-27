package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.entity.Group;
import ru.ncti.backend.entity.Sample;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 26-05-2023
 */
@Repository
public interface SampleRepository extends JpaRepository<Sample, Long> {
    List<Sample> findAllByGroup(Group group);
}
