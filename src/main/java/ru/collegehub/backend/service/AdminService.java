package ru.collegehub.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.collegehub.backend.api.request.admin.GroupRequest;
import ru.collegehub.backend.api.request.admin.SpecialtyRequest;
import ru.collegehub.backend.api.request.admin.SubjectRequest;
import ru.collegehub.backend.api.request.admin.UserRequest;
import ru.collegehub.backend.api.response.admin.GroupDetailsResponse;
import ru.collegehub.backend.api.response.admin.GroupResponse;
import ru.collegehub.backend.api.response.admin.MessageResponse;
import ru.collegehub.backend.api.response.admin.SpecialityResponse;
import ru.collegehub.backend.api.response.admin.UserDetailsResponse;
import ru.collegehub.backend.api.response.admin.UserResponse;
import ru.collegehub.backend.dto.StudentDTO;
import ru.collegehub.backend.exception.GroupNotFoundException;
import ru.collegehub.backend.exception.RoleNotFoundException;
import ru.collegehub.backend.exception.SpecialityNotFoundException;
import ru.collegehub.backend.exception.UserNotFoundException;
import ru.collegehub.backend.model.Group;
import ru.collegehub.backend.model.Role;
import ru.collegehub.backend.model.Speciality;
import ru.collegehub.backend.model.Student;
import ru.collegehub.backend.model.Subject;
import ru.collegehub.backend.model.User;
import ru.collegehub.backend.model.UserRole;
import ru.collegehub.backend.repository.GroupRepository;
import ru.collegehub.backend.repository.RoleRepository;
import ru.collegehub.backend.repository.SpecialityRepository;
import ru.collegehub.backend.repository.StudentRepository;
import ru.collegehub.backend.repository.SubjectRepository;
import ru.collegehub.backend.repository.UserRepository;
import ru.collegehub.backend.repository.UserRoleRepository;
import ru.collegehub.backend.util.PasswordGenerator;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final PasswordEncoder passwordEncoder;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final SpecialityRepository specialityRepository;
    private final SubjectRepository subjectRepository;
    private final RoleRepository roleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PasswordGenerator passwordGenerator;


    @Transactional
    public MessageResponse createUser(UserRequest request) {
        AtomicBoolean isStudent = new AtomicBoolean(false);

        List<Role> roles = request.getRoles().stream()
                .map(roleRepository::findByNameIgnoreCase)
                .filter(Optional::isPresent)
                .map(Optional::get).toList();

        String password = passwordGenerator.generate();

        User user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .patronymic(request.getPatronymic())
                .email(request.getEmail())
                .password(passwordEncoder.encode(password))
                .build();

        user.setRoles(
                roles.stream().map(
                        role -> {
                            if (role.getName().equalsIgnoreCase("student")) {
                                isStudent.set(true);
                            }
                            return UserRole.builder()
                                    .user(user)
                                    .role(role)
                                    .build();
                        }
                ).toList()
        );

        userRepository.save(user);
        userRoleRepository.saveAll(user.getRoles());

        if (isStudent.get()) {
            kafkaTemplate.send("student", StudentDTO.builder()
                    .studentId(user.getId())
                    .groupName(request.getGroupName())
                    .subgroup(request.getSubgroup())
                    .build());
        }

        return new MessageResponse("User " + request.getEmail() + " was created");
    }

    @Transactional
    public MessageResponse createSpeciality(SpecialtyRequest request) {
        var speciality = new Speciality();

        speciality.setId(request.getId());
        speciality.setName(request.getName());

        specialityRepository.save(speciality);

        return new MessageResponse("Speciality " + request.getId() + " was created");
    }

    @Transactional
    public MessageResponse createGroup(GroupRequest request) {
        var group = new Group();
        group.setCourse(request.getCourse());
        group.setName(request.getName());

        if (request.getSpeciality() != null) {
            var speciality = specialityRepository.findById(request.getSpeciality()).orElseThrow(
                    () -> new SpecialityNotFoundException(
                            "Speciality " + request.getSpeciality() + " not found"
                    )
            );
            group.setSpeciality(speciality);
        }

        groupRepository.save(group);

        return new MessageResponse("Group " + request.getName() + " was created");
    }

    @Transactional
    public MessageResponse createSubject(SubjectRequest request) {
        var subject = new Subject();
        subject.setName(request.getName());
        subjectRepository.save(subject);

        return new MessageResponse("Subject " + request.getName() + " was created");
    }

    @Async
    @KafkaListener(topics = "student", groupId = "main",
            properties = "spring.json.value.default.type=ru.collegehub.backend.dto.StudentDTO")
    @Transactional
    public void addStudent(StudentDTO dto) {
        var user = userRepository.findById(dto.getStudentId()).orElseThrow(
                () -> new UserNotFoundException("User " + dto.getStudentId() + " not found")
        );
        var group = groupRepository.findByNameIgnoreCase(dto.getGroupName()).orElseThrow(
                () -> new GroupNotFoundException("Group " + dto.getGroupName() + " not found")
        );

        var student = new Student();
        student.setUser(user);
        student.setGroup(group);
        student.setSubgroup(dto.getSubgroup());

        studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(String type) {
        if (type != null) {
            var byNameIgnoreCase = roleRepository.findByNameIgnoreCase(type).orElseThrow(
                    () -> new RoleNotFoundException("Role " + type + " not found")
            );
            return userRepository.findAllByRole(byNameIgnoreCase).stream().map(
                    user -> UserResponse.builder()
                            .id(user.getId())
                            .firstname(user.getFirstname())
                            .lastname(user.getLastname())
                            .patronymic(user.getPatronymic())
                            .roles(
                                    user.getRoles().stream()
                                            .map(role -> role
                                                    .getRole()
                                                    .getName()
                                            ).toList()
                            )
                            .build()
            ).toList();
        }
        return userRepository.findAllByQuery()
                .stream()
                .parallel()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname())
                        .patronymic(user.getPatronymic())
                        .roles(
                                user.getRoles().stream()
                                        .map(role -> role
                                                .getRole()
                                                .getName()
                                        ).toList()
                        )
                        .build()).toList();
    }

    @Transactional(readOnly = true)
    public UserDetailsResponse getUserById(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User " + id + " not found"));
        return UserDetailsResponse.builder()
                .id(id)
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .patronymic(user.getPatronymic())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(role -> role
                                .getRole()
                                .getName()
                        ).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getGroups() {
        return groupRepository.findAll().stream()
                .map(group -> new GroupResponse(group.getId(), group.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailsResponse getGroupById(Long id) {
        var group = groupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException("Group " + id + " not found"));
        return GroupDetailsResponse.builder()
                .id(group.getId())
                .course(group.getCourse())
                .name(group.getName())
                .build();
    }


    @Transactional(readOnly = true)
    public List<SpecialityResponse> getSpeciality() {
        return specialityRepository.findAll().stream().map(
                speciality -> new SpecialityResponse(speciality.getId(), speciality.getName())
        ).toList();
    }

    @Transactional(readOnly = true)
    public SpecialityResponse getSpecialityById(String id) {
        var speciality = specialityRepository.findById(id).orElseThrow(
                () -> new SpecialityNotFoundException("Speciality " + id + " not found")
        );

        return new SpecialityResponse(speciality.getId(), speciality.getName());
    }
}
