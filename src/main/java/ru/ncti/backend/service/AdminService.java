package ru.ncti.backend.service;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.ncti.backend.dto.GroupDTO;
import ru.ncti.backend.dto.ResetPasswordDTO;
import ru.ncti.backend.dto.SampleDTO;
import ru.ncti.backend.dto.SampleUploadDTO;
import ru.ncti.backend.dto.ScheduleDTO;
import ru.ncti.backend.dto.SpecialityDTO;
import ru.ncti.backend.dto.StudentDTO;
import ru.ncti.backend.dto.SubjectDTO;
import ru.ncti.backend.dto.UserDTO;
import ru.ncti.backend.entity.Group;
import ru.ncti.backend.entity.Role;
import ru.ncti.backend.entity.Sample;
import ru.ncti.backend.entity.Speciality;
import ru.ncti.backend.entity.Subject;
import ru.ncti.backend.entity.User;
import ru.ncti.backend.model.Email;
import ru.ncti.backend.repository.GroupRepository;
import ru.ncti.backend.repository.RoleRepository;
import ru.ncti.backend.repository.SampleRepository;
import ru.ncti.backend.repository.SpecialityRepository;
import ru.ncti.backend.repository.SubjectRepository;
import ru.ncti.backend.repository.UserRepository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ru.ncti.backend.dto.RabbitQueue.EMAIL_UPDATE;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 26-05-2023
 */

@Service
@RequiredArgsConstructor
@Log4j
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final SpecialityRepository specialityRepository;
    private final SubjectRepository subjectRepository;
    private final SampleRepository sampleRepository;
    private final RabbitTemplate rabbitTemplate;

    public UserDTO getProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return convert(user, UserDTO.class);
    }

    public UserDTO updateProfile(UserDTO dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        if (dto.getFirstname() != null) {
            user.setFirstname(dto.getFirstname());
        }
        if (dto.getLastname() != null) {
            user.setLastname(dto.getLastname());
        }
        if (dto.getSurname() != null) {
            user.setSurname(dto.getSurname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.save(user);
        return convert(user, UserDTO.class);
    }

    public String createStudent(StudentDTO dto) {
        User student = convert(dto, User.class);
        Role role = roleRepository.findByName("ROLE_STUDENT")
                .orElseThrow(() -> {
                    log.error("ROLE_STUDENT not found");
                    return new UsernameNotFoundException("ROLE_STUDENT not found");
                });
        Group group = groupRepository.findByName(dto.getGroup())
                .orElseThrow(() -> {
                    log.error("Group " + dto.getGroup() + " not found");
                    return new UsernameNotFoundException("Group not found");
                });
        student.setGroup(group);
        student.setPassword(passwordEncoder.encode(dto.getPassword()));
        student.setRoles(Set.of(role));

        userRepository.save(student);

        createEmailNotification(student, dto.getPassword());

        return "Студент успешно добавлен";
    }

    public String createTeacher(UserDTO dto) {
        User teacher = convert(dto, User.class);
        Role role = roleRepository.findByName("ROLE_TEACHER")
                .orElseThrow(() -> {
                    log.error("ROLE_TEACHER not found");
                    return new UsernameNotFoundException("ROLE_TEACHER not found");
                });
        teacher.setRoles(Set.of(role));
        teacher.setPassword(passwordEncoder.encode(dto.getPassword()));

        userRepository.save(teacher);

        createEmailNotification(teacher, dto.getPassword());

        return "Преподаватель успешно добавлен";
    }

    public String createSpeciality(SpecialityDTO dto) {
        if (specialityRepository.findById(dto.getId()).isPresent()) {
            log.error("Speciality " + dto.getName() + " already exist");
            throw new UsernameNotFoundException("Speciality " + dto.getName() + " already exist");
        }
        specialityRepository.save(convert(dto, Speciality.class));
        return "Специальность успешно добавлена";
    }

    public String createGroup(GroupDTO dto) throws Exception {
        if (groupRepository.findByName(dto.getName()).isPresent()) {
            log.error("Group" + dto.getName() + " already exist");
            throw new UsernameNotFoundException("Group" + dto.getName() + " already exist");
        }
        Group group = convert(dto, Group.class);
        groupRepository.save(group);
        return "Группа успешно создана";
    }

    public String createSample(SampleDTO dto) {
        Group g = groupRepository.getById(dto.getGroup());
        User teacher = userRepository.getById(dto.getTeacher());
        Subject subject = subjectRepository.getById(dto.getSubject());

        Sample sample = Sample.builder()
                .day(dto.getDay())
                .group(g)
                .numberPair(dto.getNumberPair())
                .teacher(teacher)
                .subject(subject)
                .classroom(dto.getClassroom())
                .parity(dto.getParity())
                .build();
        sampleRepository.save(sample);
        return "OK";
    }

    public String createSubject(SubjectDTO dto) {
        if (subjectRepository.findByName(dto.getName()).isPresent()) {
            log.error("Subject " + dto.getName() + " already exist");
            throw new IllegalArgumentException("Subject " + dto.getName() + " already exist");
        }
        subjectRepository.save(convert(dto, Subject.class));
        return "Предмет успешно добавлен";
    }

    public String uploadStudents(MultipartFile file) throws IOException, CsvValidationException {
        CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        List<StudentDTO> students = new CsvToBeanBuilder<StudentDTO>(csvReader)
                .withType(StudentDTO.class).build().parse();

        List<CompletableFuture<Void>> futures = students.stream()
                .map(student -> CompletableFuture.runAsync(() -> {
                    try {
                        createStudent(student);
                    } catch (UsernameNotFoundException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                })).toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();
        csvReader.close();
        log.info("Uploaded students");
        return "Uploaded students";
    }

    public String uploadTeacher(MultipartFile file) throws IOException, CsvValidationException {
        CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        List<UserDTO> teachers = new CsvToBeanBuilder<UserDTO>(csvReader)
                .withType(UserDTO.class).build().parse();

        List<CompletableFuture<Void>> futures = teachers.stream()
                .map(teacher -> CompletableFuture.runAsync(() -> {
                    try {
                        createTeacher(teacher);
                    } catch (UsernameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();
        csvReader.close();

        log.info("Uploaded teachers");
        return "Uploaded teachers";
    }

    public String uploadSchedule(MultipartFile file) throws IOException {
        CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        List<SampleUploadDTO> schedule = new CsvToBeanBuilder<SampleUploadDTO>(csvReader)
                .withType(SampleUploadDTO.class).build().parse();

        List<CompletableFuture<Void>> futures = schedule.stream()
                .map(s -> CompletableFuture.runAsync(() -> {
                    Group g = groupRepository.findByName(s.getGroup())
                            .orElseThrow(() -> {
                                log.error("Group " + s.getGroup() + " not found");
                                return new IllegalArgumentException("Group " + s.getGroup() + " not found");
                            });
                    String[] teacherName = s.getTeacher().split(" ");
                    User t = userRepository.findByLastnameAndFirstname(teacherName[0], teacherName[1])
                            .orElseThrow(() -> {
                                log.error("Teacher " + s.getTeacher() + " not found");
                                return new IllegalArgumentException("Teacher " + s.getTeacher() + " not found");
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

        log.info("Uploaded schedule");
        return "Uploaded schedule";
    }

    public List<User> getStudentsByGroup(Long group) {
        Group g = groupRepository.findById(group).orElseThrow(() -> {
            log.error("Group with id " + group + " not found");
            return new UsernameNotFoundException("Group with id " + group + " not found");
        });
        return userRepository.findAllByGroupOrderByLastname(g);
    }

    public List<User> getTeachers() {
        Role role = roleRepository.findByName("ROLE_TEACHER").orElse(null);
        return userRepository.findAllByRoles(role);
    }

    public List<Group> getGroups() {
        return groupRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.error("User with id " + id + " not found");
            return new UsernameNotFoundException("User with id + " + id + " not found");
        });
    }

    public Group getGroupById(Long id) {
        Group g = groupRepository.findById(id).orElseThrow(() -> {
            log.error("Group with id " + id + " not found");
            return new UsernameNotFoundException("Group with id " + id + " not found");
        });
        g.setSample(getTypeSchedule(g));

        return g;
    }

    public List<Subject> getSubjects() {
        return subjectRepository.findAll();
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

    public String changeSchedule(ScheduleDTO dto) {
        return null;
    }

    public String deleteGroupById(Long id) {
        Group group = groupRepository.findById(id).orElseThrow(() -> {
            log.error("Group with id " + id + " not found");
            return new UsernameNotFoundException("Group with id + " + id + " not found");
        });
        groupRepository.delete(group);
        return "Group successfully deleted";
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
        Email email = Email.builder()
                .to(dto.getEmail())
                .subject("Добро пожаловать в мобильное приложение.")
                .template("welcome-email.html")
                .properties(new HashMap<>() {{
                    put("name", dto.getFirstname());
                    put("subscriptionDate", LocalDate.now().toString());
                    put("login", dto.getUsername());
                    put("password", password);
                }})
                .build();

        rabbitTemplate.convertAndSend(EMAIL_UPDATE, email);
    }

    private <S, D> D convert(S source, Class<D> dClass) {
        return modelMapper.map(source, dClass);
    }

    public String resetPasswordForUserById(ResetPasswordDTO dto) {
        //todo: add send email with changed password
        User candidate = userRepository.findById(dto.getId()).orElseThrow(() -> {
            log.error("User with id " + dto.getPassword() + "not found");
            return new UsernameNotFoundException("User with id " + dto.getPassword() + "not found");
        });
        candidate.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(candidate);
        return "Password was reset";
    }
}
