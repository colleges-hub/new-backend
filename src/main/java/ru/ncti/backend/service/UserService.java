package ru.ncti.backend.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.ncti.backend.api.request.AuthRequest;
import ru.ncti.backend.api.request.FCMRequest;
import ru.ncti.backend.api.request.ScheduleChangeRequest;
import ru.ncti.backend.api.response.GroupResponse;
import ru.ncti.backend.api.response.ScheduleResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.PrivateChat;
import ru.ncti.backend.model.Role;
import ru.ncti.backend.model.Sample;
import ru.ncti.backend.model.Schedule;
import ru.ncti.backend.model.Subject;
import ru.ncti.backend.model.User;
import ru.ncti.backend.repository.GroupRepository;
import ru.ncti.backend.repository.PrivateChatRepository;
import ru.ncti.backend.repository.SampleRepository;
import ru.ncti.backend.repository.ScheduleRepository;
import ru.ncti.backend.repository.SubjectRepository;
import ru.ncti.backend.repository.UserRepository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.ncti.backend.config.RabbitConfig.UPDATE_SCHEDULE;

/**
 * user: ichuvilin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${minio.url}")
    private String minioURL;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupRepository groupRepository;
    private final SampleRepository sampleRepository;
    private final ScheduleRepository scheduleRepository;
    private final PrivateChatRepository privateChatRepository;
    private final SubjectRepository subjectRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MinioClient minioClient;

    public UserResponse getProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        return UserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .surname(user.getSurname())
                .email(user.getUsername())
                .role(user.getRoles())
//                .photo(String.format("%s/%s", minioURL, user.getPhoto()))
                .photo(user.getPhoto())
                .build();
    }

    public Map<String, Set<ScheduleResponse>> getSchedule() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        for (Role role : user.getRoles()) {
            if (role.getDescription().equals("Студент"))
                return getSchedule(user.getGroup().getId());
            else if (role.getDescription().equals("Преподаватель")) {
                return getScheduleFromTeacher(user);
            }
        }

        return Collections.emptyMap();
    }

    public String updateCredential(AuthRequest request) throws IllegalArgumentException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        if (request.getUsername() != null) {
            user.setEmail(request.getUsername());
        }
        if (request.getPassword() == null || request.getPassword().length() <= 5) {
            throw new IllegalArgumentException("Не удалось поменять пароль");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return "Credential was updated";
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

    public List<GroupResponse> getGroups() {
        List<Group> groups = groupRepository.findAll();

        List<GroupResponse> dtos = new ArrayList<>(groups.size());

        groups.forEach(group -> dtos
                .add(GroupResponse.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .build()));

        return dtos;
    }

    public Map<String, Set<ScheduleResponse>> getSchedule(Long id) {
        Group group = groupRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Group not found"));
        Map<String, Set<ScheduleResponse>> map = new HashMap<>();

        Set<ScheduleResponse> currSample = getTypeSchedule(group);

        for (ScheduleResponse s : currSample) {
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

                Set<ScheduleResponse> set = map.get(capitalizedDay);
                if (set != null) {
                    ScheduleResponse scheduleResponse = ScheduleResponse.builder()
                            .day(capitalizedDay)
                            .numberPair(schedule.getNumberPair())
                            .subject(schedule.getSubject().getName())
                            .data(List.of(format("%s %s %s",
                                    schedule.getTeacher().getLastname(),
                                    schedule.getTeacher().getFirstname(),
                                    schedule.getTeacher().getSurname())))
                            .classroom(schedule.getClassroom())
                            .build();
                    set.removeIf(s -> Objects.equals(s.getNumberPair(), scheduleResponse.getNumberPair()));
                    set.add(scheduleResponse);
                }
            }
        }

        sortedMap(map);

        return map;
    }

    public UserResponse getUserById(Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        User candidate = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PrivateChat privateChat = privateChatRepository.findByUser1AndUser2OrUser1AndUser2(currentUser, candidate, candidate, currentUser);

        return UserResponse.builder()
                .id(candidate.getId())
                .firstname(candidate.getFirstname())
                .lastname(candidate.getLastname())
                .surname(candidate.getSurname())
                .email(candidate.getUsername())
                .role(candidate.getRoles())
                .chat(privateChat != null ? privateChat.getId().toString() : null)
                .photo(candidate.getPhoto())
                .build();
    }

    public List<UserResponse> getUsersWithPagination(int page, int size) {
        // Создаем объект PageRequest с указанием номера страницы и размера страницы
        Pageable pageable = PageRequest.of(page, size);

        // Получаем текущего пользователя из контекста безопасности
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        // Выполняем запрос к репозиторию с использованием пагинации
        Page<User> usersPage = userRepository.findAllByOrderByLastname(pageable);

        // Фильтруем пользователей, исключая текущего пользователя (ваш аккаунт)

        List<UserResponse> users = new ArrayList<>();

        usersPage.forEach(user -> {
            if (!user.getId().equals(currentUser.getId())) {
                users.add(UserResponse.builder()
                        .id(user.getId())
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname())
                        .surname(user.getSurname())
                        .email(user.getEmail())
                        .role(user.getRoles())
                        .photo(user.getPhoto())
                        .build());
            }
        });

        return users;
    }

    public String addFCMToken(FCMRequest dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        user.setDeviceId(dto.getToken());
        userRepository.save(user);

        return "Token was added";
    }

    public String updateUserPhoto(MultipartFile file) {
        try (InputStream in = new ByteArrayInputStream(file.getBytes())) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();

            String fileName = UUID.randomUUID().toString();
            String fileExtend = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().length() - 3);

            String fileFullName = format("%s.%s", fileName, fileExtend);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileFullName)
                    .stream(in, file.getSize(), -1)
                    .build());

            user.setPhoto(fileFullName);
            userRepository.save(user);

            return "File uploaded.";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Something wrong.";
    }

    private Map<String, Set<ScheduleResponse>> getScheduleFromTeacher(User user) {
        Map<String, Set<ScheduleResponse>> map = new HashMap<>();
        List<Schedule> sch = scheduleRepository.findLatestScheduleForTeacher(user.getId());
        List<Sample> list = sampleRepository.findAllByTeacher(user);

        for (Sample sample : getTypeSchedule(list)) {
            String key = sample.getDay();
            ScheduleResponse dto = ScheduleResponse.builder()
                    .day(key)
                    .classroom(sample.getClassroom())
                    .data(List.of(sample.getGroup().getName()))
                    .numberPair(sample.getNumberPair())
                    .subject(sample.getSubject().getName())
                    .build();

            Optional<ScheduleResponse> found = map.getOrDefault(key, Collections.emptySet())
                    .stream()
                    .filter(scheduleDTO ->
                            scheduleDTO.getNumberPair().equals(dto.getNumberPair()) &&
                                    scheduleDTO.getSubject().equals(dto.getSubject()) &&
                                    scheduleDTO.getClassroom().equals(dto.getClassroom())
                    )
                    .findFirst();

            if (found.isPresent()) {
                ScheduleResponse existing = found.get();
                Set<String> groups = new HashSet<>(existing.getData());
                groups.addAll(dto.getData());
                existing.setData(new ArrayList<>(groups));
            } else {
                map.computeIfAbsent(key, k -> new HashSet<>()).add(dto);
            }
        }

        for (Schedule schedule : sch) {
            String date = LocalDate.parse(schedule.getDate().toString(), DateTimeFormatter.ISO_DATE)
                    .getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, new Locale("ru"));
            String capitalizedDay = date.substring(0, 1).toUpperCase() + date.substring(1);

            Set<ScheduleResponse> set = map.get(capitalizedDay);
            if (set != null) {
                ScheduleResponse scheduleResponse = ScheduleResponse.builder()
                        .numberPair(schedule.getNumberPair())
                        .subject(schedule.getSubject().getName())
                        .classroom(schedule.getClassroom())
                        .data(List.of(schedule.getGroup().getName()))
                        .build();
                set.removeIf(s -> Objects.equals(s.getNumberPair(), scheduleResponse.getNumberPair()));
                set.add(scheduleResponse);
            }
        }

        sortedMap(map);
        return map;
    }

    private void sortedMap(Map<String, Set<ScheduleResponse>> map) {
        map.forEach((key, value) -> {
            Set<ScheduleResponse> sortedSet = value.stream()
                    .sorted(Comparator.comparingInt(ScheduleResponse::getNumberPair))
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

    private Set<ScheduleResponse> getTypeSchedule(Group group) {
        List<Sample> sample = sampleRepository.findAllByGroup(group);
        String currentWeekType = getCurrentWeekType();
        Set<ScheduleResponse> set = new HashSet<>();

        Map<String, List<String>> mergedTeachersMap = new HashMap<>();

        sample.stream()
                .filter(s -> s.getParity().equals("0") || s.getParity().equals(currentWeekType))
                .forEach(s -> {
                    ScheduleResponse dto = convert(s);
                    String key = dto.getDay() + "-" + dto.getNumberPair() + "-" + dto.getClassroom() + "-" + dto.getSubject();
                    List<String> mergedTeachers = mergedTeachersMap.get(key);
                    if (mergedTeachers != null) {
                        mergedTeachers.addAll(dto.getData());
                    } else {
                        mergedTeachers = new ArrayList<>(dto.getData());
                        mergedTeachersMap.put(key, mergedTeachers);
                    }
                });

        mergedTeachersMap.forEach((key, mergedTeachers) -> {
            String[] parts = key.split("-");
            String day = parts[0];
            int numberPair = Integer.parseInt(parts[1]);
            String classroom = parts[2];
            String subject = parts[3];

            ScheduleResponse mergedDto = ScheduleResponse.builder()
                    .day(day)
                    .numberPair(numberPair)
                    .subject(subject)
                    .data(mergedTeachers)
                    .classroom(classroom)
                    .build();

            set.add(mergedDto);
        });

        return set;
    }

    private ScheduleResponse convert(Sample sample) {
        return ScheduleResponse.builder()
                .day(sample.getDay())
                .numberPair(sample.getNumberPair())
                .subject(sample.getSubject().getName())
                .data(List.of(format("%s %s %s",
                        sample.getTeacher().getLastname(),
                        sample.getTeacher().getFirstname(),
                        sample.getTeacher().getSurname())))
                .classroom(sample.getClassroom())
                .build();
    }

}
