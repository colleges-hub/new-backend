package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.ncti.backend.dto.ScheduleDTO;
import ru.ncti.backend.dto.UserDTO;
import ru.ncti.backend.dto.UserViewDTO;
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
import java.util.ArrayList;
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
 * user: ichuvilin
 */
@Log4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final SampleRepository sampleRepository;
    private final ScheduleRepository scheduleRepository;

    public UserViewDTO getProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User student = (User) auth.getPrincipal();
        return UserViewDTO.builder()
                .id(student.getId())
                .firstname(student.getFirstname())
                .lastname(student.getLastname())
                .surname(student.getSurname())
                .email(student.getUsername())
                .role(student.getRoles())
                .build();
    }

    public Map<String, Set<ScheduleDTO>> schedule() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User student = (User) auth.getPrincipal();
        return getSchedule(student);
    }

    public void orderCertificate() {

    }

    private Map<String, Set<ScheduleDTO>> getSchedule(User student) {
        Map<String, Set<ScheduleDTO>> map = new HashMap<>();

        Set<ScheduleDTO> currSample = getTypeSchedule(student.getGroup());

        for (ScheduleDTO s : currSample) {
            map.computeIfAbsent(s.getDay(), k -> new HashSet<>()).add(s);
        }

        List<Schedule> sch = scheduleRepository.findLatestScheduleForGroup(student.getGroup().getId());

        if (!sch.isEmpty()) {
            for (Schedule schedule : sch) {
                String date = LocalDate.parse(schedule.getDate().toString(), DateTimeFormatter.ISO_DATE).getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, new Locale("ru"));
                String capitalizedDay = date.substring(0, 1).toUpperCase() + date.substring(1);

                Set<ScheduleDTO> set = map.get(capitalizedDay);
                if (set != null) {
                    ScheduleDTO newScheduleDTO = ScheduleDTO.builder()
                            .day(capitalizedDay)
                            .numberPair(schedule.getNumberPair())
                            .subject(schedule.getSubject().getName())
                            .teachers(List.of(
                                    UserDTO.builder()
                                            .firstname(schedule.getTeacher().getFirstname())
                                            .lastname(schedule.getTeacher().getLastname())
                                            .surname(schedule.getTeacher().getSurname())
                                            .build()
                            ))
                            .classroom(schedule.getClassroom())
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

        Map<String, List<UserDTO>> mergedTeachersMap = new HashMap<>();

        sample.stream()
                .filter(s -> s.getParity().equals("0") || s.getParity().equals(currentWeekType))
                .forEach(s -> {
                    ScheduleDTO dto = convert(s);
                    String key = dto.getDay() + "-" + dto.getNumberPair() + "-" + dto.getClassroom() + "-" + dto.getSubject();
                    List<UserDTO> mergedTeachers = mergedTeachersMap.get(key);
                    if (mergedTeachers != null) {
                        mergedTeachers.addAll(dto.getTeachers());
                    } else {
                        mergedTeachers = new ArrayList<>(dto.getTeachers());
                        mergedTeachersMap.put(key, mergedTeachers);
                    }
                });

        mergedTeachersMap.forEach((key, mergedTeachers) -> {
            String[] parts = key.split("-");
            String day = parts[0];
            int numberPair = Integer.parseInt(parts[1]);
            String classroom = parts[2];
            String subject = parts[3];

            ScheduleDTO mergedDto = ScheduleDTO.builder()
                    .day(day)
                    .numberPair(numberPair)
                    .subject(subject)
                    .teachers(mergedTeachers)
                    .classroom(classroom)
                    .build();

            set.add(mergedDto);
        });

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
                .teachers(List.of(UserDTO.builder()
                        .firstname(sample.getTeacher().getFirstname())
                        .lastname(sample.getTeacher().getLastname())
                        .surname(sample.getTeacher().getSurname())
                        .build()))
                .classroom(sample.getClassroom())
                .build();
    }
}
