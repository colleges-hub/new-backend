package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ncti.backend.dto.ScheduleChangeDTO;
import ru.ncti.backend.dto.TeacherScheduleViewDTO;
import ru.ncti.backend.dto.TeacherViewDTO;
import ru.ncti.backend.entity.Group;
import ru.ncti.backend.entity.Sample;
import ru.ncti.backend.entity.Schedule;
import ru.ncti.backend.entity.Subject;
import ru.ncti.backend.entity.User;
import ru.ncti.backend.repository.GroupRepository;
import ru.ncti.backend.repository.SampleRepository;
import ru.ncti.backend.repository.ScheduleRepository;
import ru.ncti.backend.repository.SubjectRepository;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
public class TeacherService {

    private final SampleRepository sampleRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final ScheduleRepository scheduleRepository;

    public TeacherViewDTO getProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return TeacherViewDTO.builder()
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .surname(user.getSurname())
                .email(user.getEmail())
                .build();
    }

    public Map<String, Set<TeacherScheduleViewDTO>> getSchedule() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User teacher = (User) auth.getPrincipal();
        return makeSchedule(sampleRepository.findAllByTeacher(teacher));
    }

    private Map<String, Set<TeacherScheduleViewDTO>> makeSchedule(List<Sample> list) {
        Map<String, Set<TeacherScheduleViewDTO>> map = new HashMap<>();

        for (Sample s : getTypeSchedule(list)) {
            String key = s.getDay();
            TeacherScheduleViewDTO dto = TeacherScheduleViewDTO.builder()
                    .classroom(s.getClassroom())
                    .groups(List.of(s.getGroup().getName()))
                    .numberPair(s.getNumberPair())
                    .subject(s.getSubject().getName())
                    .build();

            // Проверяем наличие предмета с таким же номером пары и названием предмета
            Optional<TeacherScheduleViewDTO> found = map.getOrDefault(key, Collections.emptySet())
                    .stream()
                    .filter(scheduleDTO ->
                            scheduleDTO.getNumberPair().equals(dto.getNumberPair()) &&
                                    scheduleDTO.getSubject().equals(dto.getSubject()) &&
                                    scheduleDTO.getClassroom().equals(dto.getClassroom())
                    )
                    .findFirst();

            // Если предмет найден, объединяем группы
            if (found.isPresent()) {
                TeacherScheduleViewDTO existing = found.get();
                Set<String> groups = new HashSet<>(existing.getGroups());
                groups.addAll(dto.getGroups());
                existing.setGroups(new ArrayList<>(groups));
            } else {
                map.computeIfAbsent(key, k -> new HashSet<>()).add(dto);
            }
        }
        sortedMap(map);
        return map;
    }

    @Transactional(readOnly = false)
    public String changeSchedule(ScheduleChangeDTO dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User teacher = (User) auth.getPrincipal();
        List<Group> groups = new ArrayList<>();
        for (String gr : dto.getGroup()) {
            Group group = groupRepository.findByName(gr)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));
            groups.add(group);
        }

        Subject subject = subjectRepository.findByName(dto.getSubject())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        for (Group group : groups) {
            Schedule schedule = Schedule.builder()
                    .date(new Date())
                    .group(group)
                    .teacher(teacher)
                    .numberPair(dto.getNumberPair())
                    .subject(subject)
                    .classroom(dto.getClassroom())
                    .build();
            scheduleRepository.save(schedule);
        }

        return "Changes was added";
    }


    private void sortedMap(Map<String, Set<TeacherScheduleViewDTO>> map) {
        map.forEach((key, value) -> {
            Set<TeacherScheduleViewDTO> sortedSet = value.stream()
                    .sorted(Comparator.comparingInt(TeacherScheduleViewDTO::getNumberPair))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            map.put(key, sortedSet);
        });
    }

    private Set<Sample> getTypeSchedule(List<Sample> list) {
        String currentWeekType = getCurrentWeekType();
        return list.stream()
                .filter(s -> s.getParity().equals("0") || s.getParity().equals(currentWeekType))
                .collect(Collectors.toSet());
    }

    private String getCurrentWeekType() {
        LocalDate currentDate = LocalDate.now();
        int currentWeekNumber = currentDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        return currentWeekNumber % 2 == 0 ? "2" : "1";
    }
}
