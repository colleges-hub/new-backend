package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.ncti.backend.dto.ScheduleDTO;
import ru.ncti.backend.dto.StudentViewDTO;
import ru.ncti.backend.dto.TeacherScheduleDTO;
import ru.ncti.backend.entity.Group;
import ru.ncti.backend.entity.Sample;
import ru.ncti.backend.entity.Schedule;
import ru.ncti.backend.entity.User;
import ru.ncti.backend.repository.SampleRepository;
import ru.ncti.backend.repository.ScheduleRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 27-05-2023
 */
@Log4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final SampleRepository sampleRepository;
    private final ScheduleRepository scheduleRepository;

    public StudentViewDTO getProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User student = (User) auth.getPrincipal();
        return StudentViewDTO.builder()
                .firstname(student.getFirstname())
                .lastname(student.getLastname())
                .surname(student.getSurname())
                .email(student.getEmail())
                .group(student.getGroup().getName())
                .course(student.getGroup().getCourse())
                .speciality(student.getGroup().getSpeciality().getName())
                .build();
    }

    public Map<String, Set<ScheduleDTO>> getSchedule() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User student = (User) auth.getPrincipal();

        Map<String, Set<ScheduleDTO>> map = new HashMap<>();

        Set<ScheduleDTO> currSample = getTypeSchedule(student.getGroup());

        for (ScheduleDTO s : currSample) {
            map.computeIfAbsent(s.getDay(), k -> new HashSet<>()).add(s);
        }
        List<Schedule> sch = scheduleRepository.findLatestScheduleForGroup(student.getGroup().getId());

        if (!sch.isEmpty()) {
            for (int i = 0; i < sch.size(); i++) {
                String dayInWeek = LocalDate
                        .parse(sch.get(0).getDate().toString(), DateTimeFormatter.ISO_DATE)
                        .getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, new Locale("ru"));
                String capitalizedDay = dayInWeek.substring(0, 1).toUpperCase() + dayInWeek.substring(1);

                Set<ScheduleDTO> set = map.get(capitalizedDay);
                if (set != null) {
                    ScheduleDTO newScheduleDTO = ScheduleDTO.builder()
                            .day(capitalizedDay)
                            .numberPair(sch.get(i).getNumberPair())
                            .subject(sch.get(i).getSubject().getName())
                            .teacher(
                                    new TeacherScheduleDTO(
                                            sch.get(i).getTeacher().getFirstname(),
                                            sch.get(i).getTeacher().getLastname(),
                                            sch.get(i).getTeacher().getSurname()
                                    )
                            )
                            .classroom(sch.get(i).getClassroom())
                            .build();
                    set.removeIf(s -> Objects.equals(s.getNumberPair(), newScheduleDTO.getNumberPair()));
                    set.add(newScheduleDTO);
                }
            }
        }

        map.forEach((key, value) -> {
            Set<ScheduleDTO> sortedSet = value.stream()
                    .sorted(Comparator.comparingInt(ScheduleDTO::getNumberPair))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            map.put(key, sortedSet);
        });

        return map;
    }

    private Set<ScheduleDTO> getTypeSchedule(Group group) {
        List<Sample> sample = sampleRepository.findAllByGroup(group);
        String currentWeekType = getCurrentWeekType();
        Set<ScheduleDTO> set = new HashSet<>();

        sample.stream()
                .filter(s -> s.getParity().equals("0") || s.getParity().equals(currentWeekType))
                .forEach(s -> set.add(convert(s)));

        return set;
    }

    private String getCurrentWeekType() {
        LocalDate currentDate = LocalDate.now();
        int currentWeekNumber = currentDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        return currentWeekNumber % 2 == 0 ? "2" : "1";
    }

    private ScheduleDTO convert(Sample sample) {
        return ScheduleDTO.builder()
                .day(sample.getDay())
                .numberPair(sample.getNumberPair())
                .subject(sample.getSubject().getName())
                .teacher(new TeacherScheduleDTO(
                        sample.getTeacher().getFirstname(),
                        sample.getTeacher().getLastname(),
                        sample.getTeacher().getSurname()
                ))
                .classroom(sample.getClassroom())
                .build();
    }
}
