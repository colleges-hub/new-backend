package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.Sample;
import ru.ncti.backend.model.User;

import java.util.List;

@Repository
public interface SampleRepository extends JpaRepository<Sample, Long> {
    List<Sample> findAllByTeacher(User user);

    List<Sample> findAllByGroup(Group group);
}
