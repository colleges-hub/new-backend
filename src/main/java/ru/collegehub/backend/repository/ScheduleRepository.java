package ru.collegehub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Schedule;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query(value = "SELECT * FROM raspisanie as s " +
                   "WHERE s.id_group = :groupId " +
                   "AND s.date >= CURRENT_DATE " +
                   "AND s.date <= CURRENT_DATE + INTERVAL '5 days'",
            nativeQuery = true)
    List<Schedule> findLatestScheduleForGroup(Long groupId);

    @Query(value = "SELECT * FROM raspisanie as s " +
                   "WHERE s.teacher_id = :teacher " +
                   "AND s.date >= CURRENT_DATE - INTERVAL '5 days' " +
                   "AND s.date <= CURRENT_DATE",
            nativeQuery = true)
    List<Schedule> findLatestScheduleForTeacher(Long teacher);

}
