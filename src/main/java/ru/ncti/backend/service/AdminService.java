package ru.ncti.backend.service;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.ncti.backend.api.request.AuthRequest;
import ru.ncti.backend.api.request.GroupRequest;
import ru.ncti.backend.api.request.ScheduleRequest;
import ru.ncti.backend.api.request.SectionRequest;
import ru.ncti.backend.api.request.SpecialityRequest;
import ru.ncti.backend.api.request.SubjectRequest;
import ru.ncti.backend.api.request.TemplateRequest;
import ru.ncti.backend.api.request.UploadTemplateRequest;
import ru.ncti.backend.api.request.UploadUserRequest;
import ru.ncti.backend.api.request.UserRequest;
import ru.ncti.backend.api.response.EmailResponse;
import ru.ncti.backend.api.response.GroupResponse;
import ru.ncti.backend.api.response.RoleResponse;
import ru.ncti.backend.api.response.SectionResponse;
import ru.ncti.backend.api.response.SpecialityResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.Role;
import ru.ncti.backend.model.Sample;
import ru.ncti.backend.model.Schedule;
import ru.ncti.backend.model.Section;
import ru.ncti.backend.model.Speciality;
import ru.ncti.backend.model.Subject;
import ru.ncti.backend.model.User;
import ru.ncti.backend.repository.GroupRepository;
import ru.ncti.backend.repository.RoleRepository;
import ru.ncti.backend.repository.SampleRepository;
import ru.ncti.backend.repository.ScheduleRepository;
import ru.ncti.backend.repository.SectionRepository;
import ru.ncti.backend.repository.SpecialityRepository;
import ru.ncti.backend.repository.SubjectRepository;
import ru.ncti.backend.repository.UserRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ru.ncti.backend.config.RabbitConfig.EMAIL_UPDATE;


/**
 * user: ichuvilin
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final SpecialityRepository specialityRepository;
    private final SubjectRepository subjectRepository;
    private final SampleRepository sampleRepository;
    private final SectionRepository sectionRepository;
    private final ScheduleRepository scheduleRepository;
    private final RabbitTemplate rabbitTemplate;


    public UserResponse getProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        UserResponse response = convert(user, UserResponse.class);
        if (user.getPhoto() != null)
            response.setPhoto(user.getPhoto());
        response.setRole(user.getRoles());

        log.info(String.format("Admin %s show your profile", user.getUsername()));

        return response;
    }

    @Transactional(readOnly = false)
    public String createUser(UserRequest dto) {
        User user = convert(dto, User.class);
        Set<Role> roles = new HashSet<>(dto.getRole().size());
        for (String role : dto.getRole()) {
            Role rl = roleRepository.findByDescription(role)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Role %s not found", role)));
            roles.add(rl);
        }
        if (dto.getGroup() != null && !dto.getGroup().isEmpty()) {
            Group group = groupRepository.findByName(dto.getGroup())
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Group %s not found", dto.getGroup())));
            user.setGroup(group);
        }
        String randomPass = UUID.randomUUID().toString();
        user.setRoles(roles);
        user.setPassword(passwordEncoder.encode(randomPass));
        userRepository.save(user);
        createEmailNotification(user, randomPass);
        return "User was added";
    }

    public List<SectionResponse> getSections() {
        return sectionRepository.findAll().stream().map(
                section -> SectionResponse.builder()
                        .name(section.getName())
                        .email(section.getEmail())
                        .build()
        ).toList();
    }

    public String createSection(SectionRequest request) {
        Section section = modelMapper.map(request, Section.class);
        sectionRepository.save(section);
        return "Section was added";
    }

    public String createGroup(GroupRequest request) {
        if (groupRepository.findByName(request.getName()).isPresent()) {
            log.error(String.format("Group %s already exist", request.getName()));
            throw new IllegalArgumentException(String.format("Group %s already exist", request.getName()));
        }
        Group group = new Group();
        group.setName(request.getName());
        group.setCourse(request.getCourse());
        Speciality speciality = specialityRepository.findById(request.getSpeciality())
                .orElseThrow(() -> new IllegalArgumentException(String.format("Speciality %s not found", request.getSpeciality())));
        Section section = sectionRepository.findByName(request.getSection())
                .orElseThrow(() -> new IllegalArgumentException(String.format("Section %s not found", request.getSpeciality())));
        log.info(section.getName());
        group.setSpeciality(speciality);
        group.setSection(section);
        groupRepository.save(group);
        return "Group was added";
    }

    public List<RoleResponse> getRoles() {
        return roleRepository.findAll().stream().map(
                role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getDescription())
                        .build()
        ).toList();
    }

    public List<Subject> getSubjects() {
        return subjectRepository.findAllByOrderByName();
    }

    // TODO: all rework
    public String updateProfile(AuthRequest dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        if (dto.getUsername() != null) {
            user.setEmail(dto.getUsername());
        }
        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.save(user);
        return "Data updated";
    }

    public String createTemplate(TemplateRequest request) {
        log.info(request.toString());
        Group g = groupRepository.findById(request.getGroup())
                .orElseThrow(() -> {
                    log.error(String.format("Group %d already exist", request.getGroup()));
                    return new IllegalArgumentException(String.format("Group %d already exist", request.getGroup()));
                });
        User teacher = userRepository.findById(request.getTeacher())
                .orElseThrow(() -> {
                    log.error(String.format("Teacher %d already exist", request.getTeacher()));
                    return new IllegalArgumentException(String.format("Teacher %d already exist", request.getTeacher()));
                });
        Subject subject = subjectRepository.findById(request.getSubject())
                .orElseThrow(() -> {
                    log.error(String.format("Subject %d already exist", request.getSubject()));
                    return new IllegalArgumentException(String.format("Subject %d already exist", request.getSubject()));
                });

        Sample sample = Sample.builder()
                .day(request.getDay())
                .group(g)
                .numberPair(request.getNumberPair())
                .teacher(teacher)
                .subject(subject)
                .classroom(request.getClassroom())
                .parity(request.getParity())
                .build();
        sampleRepository.save(sample);
        return "Template was added";
    }

    public String createSubject(SubjectRequest request) {
        if (subjectRepository.findByName(request.getName()).isPresent()) {
            log.error(String.format("Subject %s already exist", request.getName()));
            throw new IllegalArgumentException(String.format("Subject %s already exist", request.getName()));
        }
        subjectRepository.save(convert(request, Subject.class));
        return "Subject was added";
    }


    public String uploadSubjects(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            List<SubjectRequest> subjects = new CsvToBeanBuilder<SubjectRequest>(csvReader)
                    .withType(SubjectRequest.class).build().parse();

            List<CompletableFuture<Void>> futures = subjects.stream().map(
                    subject -> CompletableFuture.runAsync(() -> createSubject(subject))
            ).toList();
        }

        return "Subjects upload";
    }

    @Transactional(readOnly = false)
    public String uploadUsers(MultipartFile file) throws IOException, CsvValidationException {

        try (InputStream inputStream = file.getInputStream()) {
            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            List<UploadUserRequest> users = new CsvToBeanBuilder<UploadUserRequest>(csvReader)
                    .withType(UploadUserRequest.class).build().parse();

            List<CompletableFuture<Void>> futures = users.stream()
                    .map(user -> CompletableFuture.runAsync(() -> {
                        try {
                            UserRequest usr = convert(user, UserRequest.class);
                            usr.setRole(List.of(user.getRole()));
                            createUser(usr);
                        } catch (IllegalArgumentException e) {
                            log.error(e.getMessage());
                            throw new IllegalArgumentException(e);
                        }
                    })).toList();

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.join();
            csvReader.close();
        }

        return "Uploaded users";
    }

    public String uploadTemplate(MultipartFile file) throws IOException {

        try (InputStream inputStream = file.getInputStream()) {
            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            List<UploadTemplateRequest> schedule = new CsvToBeanBuilder<UploadTemplateRequest>(csvReader)
                    .withType(UploadTemplateRequest.class).build().parse();

            List<CompletableFuture<Void>> futures = schedule.stream()
                    .map(s -> CompletableFuture.runAsync(() -> {
                        Group g = groupRepository.findByName(s.getGroup())
                                .orElseThrow(() -> {
                                    log.error(String.format("Group %s not found", s.getGroup()));
                                    return new IllegalArgumentException(String.format("Group %s not found", s.getGroup()));
                                });
                        String[] teacherName = s.getTeacher().split(" ");
                        User t = userRepository.findByLastnameAndFirstname(teacherName[0], teacherName[1])
                                .orElseThrow(() -> {
                                    log.error(String.format("Teacher %s not found", s.getTeacher()));
                                    return new IllegalArgumentException(String.format("Teacher %s not found", s.getTeacher()));
                                });

                        Sample sch = convert(s, Sample.class);
                        Subject subject = subjectRepository.findByName(s.getSubject()).orElse(null);
                        sch.setGroup(g);
                        sch.setTeacher(t);
                        sch.setParity(s.getWeekType());
                        sch.setSubject(subject);
                        sampleRepository.save(sch);
                    })).toList();

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.join();
            csvReader.close();
        }

        return "Uploaded schedule";
    }

    public List<UserResponse> getStudentsByGroup(Long group) {
        Group g = groupRepository.findById(group).orElseThrow(() -> {
            log.error(String.format("Group %s not found", group));
            return new IllegalArgumentException("Group not found");
        });
        return userRepository.findAllByGroupOrderByLastname(g)
                .stream().map(student -> UserResponse.builder()
                        .id(student.getId())
                        .firstname(student.getFirstname())
                        .lastname(student.getLastname())
                        .surname(student.getSurname())
                        .email(student.getEmail())
                        .role(student.getRoles())
                        .build()).toList();
    }


    public List<UserResponse> getUsersByType(String type) {
        Role role = null;
        if (type.equalsIgnoreCase("student")) {
            role = roleRepository.findByName("ROLE_STUDENT")
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        } else if (type.equalsIgnoreCase("teacher")) {
            role = roleRepository.findByName("ROLE_TEACHER")
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        }
        assert role != null;

        return userRepository.findAllByRolesOrderByLastname(role).stream().map(
                user -> UserResponse.builder()
                        .id(user.getId())
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname())
                        .surname(user.getSurname())
                        .email(user.getEmail())
                        .role(user.getRoles())
                        .build()).toList();
    }

    public List<GroupResponse> getGroups() {
        return groupRepository.findAll().stream().map(
                group -> GroupResponse.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .course(group.getCourse())
                        .speciality(group.getSpeciality() != null ?
                                String.format("%s%s", group.getSpeciality().getId(), group.getSpeciality().getName()) : "")
                        .build()
        ).toList();
    }

    public UserResponse getUserById(Long id) {
        return convert(userRepository.findById(id).orElseThrow(() -> {
            log.error(String.format("User with id %d not found", id));
            return new IllegalArgumentException(String.format("User with id %d not found", id));
        }), UserResponse.class);
    }

    // todo: think how to change this
    public Group getGroupById(Long id) {
        Group g = groupRepository.findById(id).orElseThrow(() -> {
            log.error(String.format("Group with id %d not found", id));
            return new IllegalArgumentException(String.format("Group with id %d not found", id));
        });
        g.setSample(getTypeSchedule(g));

        return g;
    }

    public Subject getSubjectById(Long id) {
        return subjectRepository.findById(id).orElse(null);
    }

    public String deleteUserById(Long id) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(String.format("User with id %d not found", id)));

        student.getRoles().forEach(r -> r.getUsers().remove(student));
        student.getRoles().clear();
        userRepository.save(student);

        userRepository.delete(student);
        return "User successfully deleted";
    }

    public String createSpeciality(SpecialityRequest dto) {
        if (specialityRepository.findById(dto.getId()).isPresent()) {
            log.error(String.format("Speciality %s already exist", dto.getName()));
            throw new IllegalArgumentException(String.format("Speciality %s already exist", dto.getName()));
        }
        specialityRepository.save(convert(dto, Speciality.class));
        return "Speciality was added";
    }

    public String uploadSpeciality(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            List<SpecialityRequest> specialities = new CsvToBeanBuilder<SpecialityRequest>(csvReader)
                    .withType(SpecialityRequest.class).build().parse();

            List<CompletableFuture<Void>> futures = specialities
                    .stream()
                    .map(s -> CompletableFuture.runAsync(() -> createSpeciality(s))).toList();

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.join();
            csvReader.close();
        }

        return "Upload specialities";
    }

    public List<SpecialityResponse> getSpecialities() {
        return specialityRepository.findAll().stream().map(
                speciality -> SpecialityResponse.builder()
                        .id(speciality.getId())
                        .name(speciality.getName())
                        .build()
        ).toList();
    }

    //todo
    public String changeSchedule(ScheduleRequest request) {
        log.info(request.toString());

        Group group = groupRepository.findById(request.getGroup()).orElseThrow(() -> {
            log.error(String.format("Group with id %d not found", request.getGroup()));
            return new IllegalArgumentException(String.format("Group with id %d not found", request.getGroup()));
        });

        Subject subject = subjectRepository.findById(request.getSubject()).orElseThrow(() -> {
            log.error(String.format("Subject with id %d not found", request.getSubject()));
            return new IllegalArgumentException(String.format("Subject with id %d not found", request.getSubject()));
        });

        User teacher = userRepository.findById(request.getTeacher()).orElseThrow(() -> {
            log.error(String.format("Teacher with id %d not found", request.getSubject()));
            return new IllegalArgumentException(String.format("Teacher with id %d not found", request.getSubject()));
        });


        Schedule schedule = Schedule.builder()
                .date(request.getDate())
                .group(group)
                .subject(subject)
                .numberPair(request.getNumberPair())
                .teacher(teacher)
                .classroom(request.getClassroom())
                .build();

        scheduleRepository.save(schedule);

        return "Changes was added";
    }

    public String deleteGroupById(Long id) {
        Group group = groupRepository.findById(id).orElseThrow(() -> {
            log.error(String.format("Group with id %d not found", id));
            return new IllegalArgumentException(String.format("Group with id %d not found", id));
        });
        groupRepository.delete(group);
        return "Group successfully deleted";
    }

    public String updateCredentialUserById(Long id, AuthRequest request) {
        //todo: add send email with changed password
        User candidate = userRepository.findById(id).orElseThrow(() -> {
            log.error("User with id " + request.getPassword() + "not found");
            return new UsernameNotFoundException("User with id " + request.getPassword() + "not found");
        });
        if (request.getUsername() != null) {
            candidate.setEmail(request.getUsername());
        }
        if (request.getPassword() != null) {
            candidate.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(candidate);
        return "User credential was update";
    }

    private Set<Sample> getTypeSchedule(Group group) {
        List<Sample> sample = sampleRepository.findAllByGroup(group);
        String currentWeekType = getCurrentWeekType();
        return sample.stream()
                .filter(s -> s.getParity().equals("0") || s.getParity().equals(currentWeekType))
                .collect(Collectors.toSet());
    }

    private String getCurrentWeekType() {
        LocalDate currentDate = LocalDate.now();
        int currentWeekNumber = currentDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        return currentWeekNumber % 2 == 0 ? "2" : "1";
    }

    private void createEmailNotification(User dto, String password) {
        EmailResponse email = EmailResponse.builder()
                .to(dto.getEmail())
                .subject("Добро пожаловать в приложение для колледжа!")
                .template("welcome-email.html")
                .properties(new HashMap<>() {{
                    put("login", dto.getUsername());
                    put("password", password);
                }})
                .build();

        rabbitTemplate.convertAndSend(EMAIL_UPDATE, email);
    }

    private <S, D> D convert(S source, Class<D> dClass) {
        return modelMapper.map(source, dClass);
    }
}
