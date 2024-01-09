package ru.collegehub.backend.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.collegehub.backend.model.Group;
import ru.collegehub.backend.model.Schedule;
import ru.collegehub.backend.model.User;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Cacheable(value = "scheduleByGroupOnCurrentWeekCache", key = "#group.id")
    @Query(value = """
            SELECT s FROM Schedule s
                JOIN FETCH s.teacher
                JOIN FETCH s.subject
                WHERE s.group = :group
                AND EXTRACT(YEAR FROM s.dayOfWeek) = EXTRACT(YEAR FROM CURRENT_DATE)
                AND EXTRACT(WEEK FROM s.dayOfWeek) = EXTRACT(WEEK FROM CURRENT_DATE)
            """)
    List<Schedule> findScheduleByGroupOnCurrentWeek(Group group);


    @Query(value = """
            SELECT s FROM Schedule s
                    JOIN FETCH s.group
                    JOIN FETCH s.subject
                    WHERE s.teacher = :teacher
                    AND EXTRACT(YEAR FROM s.dayOfWeek) = EXTRACT(YEAR FROM CURRENT_DATE)
                    AND EXTRACT(WEEK FROM s.dayOfWeek) = EXTRACT(WEEK FROM CURRENT_DATE)
            """)
    List<Schedule> findScheduleByTeacherOnCurrentWeek(User teacher);
}
