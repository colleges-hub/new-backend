package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ncti.backend.api.request.ScheduleChangeRequest;
import ru.ncti.backend.api.response.TeacherScheduleResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.Sample;
import ru.ncti.backend.model.Schedule;
import ru.ncti.backend.model.Subject;
import ru.ncti.backend.model.User;
import ru.ncti.backend.repository.GroupRepository;
import ru.ncti.backend.repository.SampleRepository;
import ru.ncti.backend.repository.ScheduleRepository;
import ru.ncti.backend.repository.SubjectRepository;
import ru.ncti.backend.util.UserUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.ncti.backend.config.RabbitConfig.UPDATE_SCHEDULE;


/**
 * user: ichuvilin
 */
@Log4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final SampleRepository sampleRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final ScheduleRepository scheduleRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UserUtil userUtil;

    public UserResponse getProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return UserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .surname(user.getSurname())
                .email(user.getEmail())
                .role(user.getRoles())
                .photo(user.getPhoto())
                .build();
    }

    public Map<String, Set<TeacherScheduleResponse>> schedule() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User teacher = (User) auth.getPrincipal();
        return makeSchedule(sampleRepository.findAllByTeacher(teacher), teacher.getId());
    }

    private Map<String, Set<TeacherScheduleResponse>> makeSchedule(List<Sample> list, Long teacherId) {
        Map<String, Set<TeacherScheduleResponse>> map = new HashMap<>();
        List<Schedule> sch = scheduleRepository.findLatestScheduleForTeacher(teacherId);

        for (Sample sample : getTypeSchedule(list)) {
            String key = sample.getDay();
            TeacherScheduleResponse dto = TeacherScheduleResponse.builder()
                    .classroom(sample.getClassroom())
                    .groups(List.of(sample.getGroup().getName()))
                    .numberPair(sample.getNumberPair())
                    .subject(sample.getSubject().getName())
                    .build();

            Optional<TeacherScheduleResponse> found = map.getOrDefault(key, Collections.emptySet())
                    .stream()
                    .filter(scheduleDTO ->
                            scheduleDTO.getNumberPair().equals(dto.getNumberPair()) &&
                                    scheduleDTO.getSubject().equals(dto.getSubject()) &&
                                    scheduleDTO.getClassroom().equals(dto.getClassroom())
                    )
                    .findFirst();

            if (found.isPresent()) {
                TeacherScheduleResponse existing = found.get();
                Set<String> groups = new HashSet<>(existing.getGroups());
                groups.addAll(dto.getGroups());
                existing.setGroups(new ArrayList<>(groups));
            } else {
                map.computeIfAbsent(key, k -> new HashSet<>()).add(dto);
            }
        }

        for (Schedule schedule : sch) {
            String date = LocalDate.parse(schedule.getDate().toString(), DateTimeFormatter.ISO_DATE)
                    .getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, new Locale("ru"));
            String capitalizedDay = date.substring(0, 1).toUpperCase() + date.substring(1);

            Set<TeacherScheduleResponse> set = map.get(capitalizedDay);
            if (set != null) {
                TeacherScheduleResponse newScheduleDTO = TeacherScheduleResponse.builder()
                        .numberPair(schedule.getNumberPair())
                        .subject(schedule.getSubject().getName())
                        .classroom(schedule.getClassroom())
                        .groups(List.of(schedule.getGroup().getName()))
                        .build();
                set.removeIf(s -> Objects.equals(s.getNumberPair(), newScheduleDTO.getNumberPair()));
                set.add(newScheduleDTO);
            }
        }

        sortedMap(map);
        return map;
    }

    @Transactional(readOnly = false)
    public String changeSchedule(ScheduleChangeRequest request) throws ParseException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User teacher = (User) auth.getPrincipal();

        List<Group> groups = new ArrayList<>();
        for (String gr : request.getGroup()) {
            Group group = groupRepository.findByName(gr)
                    .orElseThrow(() -> {
                        log.error(String.format("Group %s not found", gr));
                        return new IllegalArgumentException(String.format("Group %s not found", gr));
                    });
            groups.add(group);
        }

        Subject subject = subjectRepository.findByName(request.getSubject())
                .orElseThrow(() -> {
                    log.error(String.format("Subject %s not found", request.getSubject()));
                    return new IllegalArgumentException(String.format("Subject %s not found", request.getSubject()));
                });

        SimpleDateFormat format = new SimpleDateFormat();
        format.applyPattern("yyyy-MM-dd");

        for (Group group : groups) {
            Schedule schedule = Schedule.builder()
                    .date(format.parse(request.getDate().split("T")[0]))
                    .group(group)
                    .teacher(teacher)
                    .numberPair(request.getNumberPair())
                    .subject(subject)
                    .classroom(request.getClassroom())
                    .build();
            rabbitTemplate.convertAndSend(UPDATE_SCHEDULE,
                    new HashMap<>() {{
                        put("date", request.getDate());
                        put("group", schedule.getGroup().getName());
                        put("pair", schedule.getNumberPair());
                        put("classroom", schedule.getClassroom());
                    }});
            scheduleRepository.save(schedule);
        }
        return "Changes was added";
    }


    private void sortedMap(Map<String, Set<TeacherScheduleResponse>> map) {
        map.forEach((key, value) -> {
            Set<TeacherScheduleResponse> sortedSet = value.stream()
                    .sorted(Comparator.comparingInt(TeacherScheduleResponse::getNumberPair))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            map.put(key, sortedSet);
        });
    }

    private Set<Sample> getTypeSchedule(List<Sample> list) {
        String currentWeekType = userUtil.getCurrentWeekType();
        return list.stream()
                .filter(s -> s.getParity().equals("0") || s.getParity().equals(currentWeekType))
                .collect(Collectors.toSet());
    }
}
