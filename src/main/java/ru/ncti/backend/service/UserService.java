package ru.ncti.backend.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.ncti.backend.api.request.AuthRequest;
import ru.ncti.backend.api.request.FCMRequest;
import ru.ncti.backend.api.response.GroupResponse;
import ru.ncti.backend.api.response.ScheduleResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.PrivateChat;
import ru.ncti.backend.model.Schedule;
import ru.ncti.backend.model.User;
import ru.ncti.backend.repository.GroupRepository;
import ru.ncti.backend.repository.PrivateChatRepository;
import ru.ncti.backend.repository.ScheduleRepository;
import ru.ncti.backend.repository.UserRepository;
import ru.ncti.backend.util.UserUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * user: ichuvilin
 */
@Log4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupRepository groupRepository;
    private final ScheduleRepository scheduleRepository;
    private final PrivateChatRepository privateChatRepository;
    private final MinioClient minioClient;

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

        Set<ScheduleResponse> currSample = userUtil.getTypeSchedule(group);

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
                    ScheduleResponse newScheduleResponse = ScheduleResponse.builder()
                            .day(capitalizedDay)
                            .numberPair(schedule.getNumberPair())
                            .subject(schedule.getSubject().getName())
                            .teachers(List.of(
                                    UserResponse.builder()
                                            .firstname(schedule.getTeacher().getFirstname())
                                            .lastname(schedule.getTeacher().getLastname())
                                            .surname(schedule.getTeacher().getSurname())
                                            .photo(schedule.getTeacher().getPhoto())
                                            .build()
                            ))
                            .classroom(schedule.getClassroom())
                            .build();
                    set.removeIf(s -> Objects.equals(s.getNumberPair(), newScheduleResponse.getNumberPair()));
                    set.add(newScheduleResponse);
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

    public List<UserResponse> getUsers() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        final List<UserResponse> users = new ArrayList<>();

        userRepository.findAll().forEach(user -> {
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
            String fileExtend = file.getOriginalFilename().substring(file.getOriginalFilename().length() - 3);

            String fileFullName = String.format("%s.%s", fileName, fileExtend);

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

}
