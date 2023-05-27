package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.entity.Schedule;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query(value = "SELECT * FROM raspisanie as s WHERE s.id_group = :groupId AND s.date = (SELECT MAX(date) FROM raspisanie WHERE id_group = :groupId AND date >= CURRENT_DATE)", nativeQuery = true)
    List<Schedule> findLatestScheduleForGroup(Long groupId);
}
