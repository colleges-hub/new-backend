package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.ncti.backend.api.response.ScheduleResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.model.Schedule;
import ru.ncti.backend.model.User;
import ru.ncti.backend.repository.ScheduleRepository;
import ru.ncti.backend.util.UserUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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

    private final UserUtil userUtil;
    private final ScheduleRepository scheduleRepository;

    public UserResponse getProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User student = (User) auth.getPrincipal();

        return UserResponse.builder()
                .id(student.getId())
                .firstname(student.getFirstname())
                .lastname(student.getLastname())
                .surname(student.getSurname())
                .email(student.getUsername())
                .role(student.getRoles())
                .photo(student.getPhoto())
                .build();
    }

    public Map<String, Set<ScheduleResponse>> schedule() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User student = (User) auth.getPrincipal();
        return getSchedule(student);
    }

    public void orderCertificate() {
        // todo
    }

    private Map<String, Set<ScheduleResponse>> getSchedule(User student) {
        Map<String, Set<ScheduleResponse>> map = new HashMap<>();

        Set<ScheduleResponse> currSample = userUtil.getTypeSchedule(student.getGroup());

        for (ScheduleResponse s : currSample) {
            map.computeIfAbsent(s.getDay(), k -> new HashSet<>()).add(s);
        }

        List<Schedule> sch = scheduleRepository.findLatestScheduleForGroup(student.getGroup().getId());

        if (!sch.isEmpty()) {
            for (Schedule schedule : sch) {
                String date = LocalDate.parse(schedule.getDate().toString(), DateTimeFormatter.ISO_DATE).getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, new Locale("ru"));
                String capitalizedDay = date.substring(0, 1).toUpperCase() + date.substring(1);

                Set<ScheduleResponse> set = map.get(capitalizedDay);
                if (set != null) {
                    ScheduleResponse newScheduleDTO = ScheduleResponse.builder()
                            .day(capitalizedDay)
                            .numberPair(schedule.getNumberPair())
                            .subject(schedule.getSubject().getName())
                            .teachers(List.of(
                                    UserResponse.builder()
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
            Set<ScheduleResponse> sortedSet = value.stream()
                    .sorted(Comparator.comparingInt(ScheduleResponse::getNumberPair))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            map.put(key, sortedSet);
        });

        return map;
    }

}
