package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.ncti.backend.dto.ChangePasswordDTO;
import ru.ncti.backend.dto.FcmDTO;
import ru.ncti.backend.dto.GroupViewDTO;
import ru.ncti.backend.dto.ScheduleDTO;
import ru.ncti.backend.dto.UserDTO;
import ru.ncti.backend.dto.UserViewDTO;
import ru.ncti.backend.entity.Group;
import ru.ncti.backend.entity.Sample;
import ru.ncti.backend.entity.Schedule;
import ru.ncti.backend.entity.User;
import ru.ncti.backend.repository.GroupRepository;
import ru.ncti.backend.repository.RoleRepository;
import ru.ncti.backend.repository.SampleRepository;
import ru.ncti.backend.repository.ScheduleRepository;
import ru.ncti.backend.repository.UserRepository;

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
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 27-05-2023
 */
@Log4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupRepository groupRepository;
    private final SampleRepository sampleRepository;
    private final ScheduleRepository scheduleRepository;
    private final RedisService redisService;

    public String changePassword(ChangePasswordDTO dto) throws IllegalArgumentException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        if (dto.getPassword() == null || dto.getPassword().length() <= 5) {
            throw new IllegalArgumentException("Не удалось поменять пароль");
        }

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
        return "Пароль успешно изменен";
    }


    public List<GroupViewDTO> getGroups() {
        List<Group> groups = groupRepository.findAll();

        List<GroupViewDTO> dtos = new ArrayList<>(groups.size());

        groups.forEach(group -> dtos
                .add(GroupViewDTO.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .build()));

        return dtos;
    }

    public Map<String, Set<ScheduleDTO>> getSchedule(Long id) {
        Group group = groupRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Group not found"));
        Map<String, Set<ScheduleDTO>> map = new HashMap<>();

        Set<ScheduleDTO> currSample = getTypeSchedule(group);

        for (ScheduleDTO s : currSample) {
            map.computeIfAbsent(s.getDay(), k -> new HashSet<>()).add(s);
        }
        List<Schedule> sch = scheduleRepository.findLatestScheduleForGroup(group.getId());

        if (!sch.isEmpty()) {
            for (Schedule schedule : sch) {
                String dayInWeek = LocalDate
                        .parse(sch.get(0).getDate().toString(), DateTimeFormatter.ISO_DATE)
                        .getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, new Locale("ru"));
                String capitalizedDay = dayInWeek.substring(0, 1).toUpperCase() + dayInWeek.substring(1);

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

    public List<UserViewDTO> getUsers(String type) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        final List<UserViewDTO> users = new ArrayList<>();

        if (type == null) {
            userRepository.findAll().forEach(user -> {
                if (!user.getId().equals(currentUser.getId()) && user.getId() != 1) {
                    users.add(UserViewDTO.builder()
                            .id(user.getId())
                            .firstname(user.getFirstname())
                            .lastname(user.getLastname())
                            .surname(user.getSurname())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .build());
                }
            });
        } else if (type.equals("student")) {
            roleRepository.findByName("ROLE_STUDENT")
                    .ifPresent(role -> {
                        userRepository
                                .findAllByRoles(role)
                                .forEach(s -> {
                                    if (!s.getId().equals(currentUser.getId()) && s.getId() != 1) {
                                        users.add(UserViewDTO.builder()
                                                .id(s.getId())
                                                .firstname(s.getFirstname())
                                                .lastname(s.getLastname())
                                                .surname(s.getSurname())
                                                .email(s.getEmail())
                                                .username(s.getUsername())
                                                .build());
                                    }
                                });
                    });
        } else if (type.equals("teacher")) {
            roleRepository.findByName("ROLE_TEACHER")
                    .ifPresent(role -> {
                        userRepository
                                .findAllByRoles(role)
                                .forEach(s -> {
                                    if (!s.getId().equals(currentUser.getId()) && s.getId() != 1) {
                                        users.add(UserViewDTO.builder()
                                                .id(s.getId())
                                                .firstname(s.getFirstname())
                                                .lastname(s.getLastname())
                                                .surname(s.getSurname())
                                                .email(s.getEmail())
                                                .username(s.getUsername())
                                                .build());
                                    }
                                });
                    });
        }

        return users;
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

    public String addFCMToken(FcmDTO dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        log.info(dto.getToken());
        log.info(currentUser.getUsername());

        redisService.setValueSet(String.format("device:%s", currentUser.getUsername()), dto.getToken());

        return "Token was added";
    }
}
