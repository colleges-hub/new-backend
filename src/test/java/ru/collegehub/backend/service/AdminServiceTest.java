package ru.collegehub.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.collegehub.backend.api.request.admin.GroupRequest;
import ru.collegehub.backend.api.request.admin.ScheduleRequest;
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
import ru.collegehub.backend.model.Group;
import ru.collegehub.backend.model.Role;
import ru.collegehub.backend.model.Speciality;
import ru.collegehub.backend.model.Student;
import ru.collegehub.backend.model.Subject;
import ru.collegehub.backend.model.User;
import ru.collegehub.backend.model.UserRole;
import ru.collegehub.backend.repository.GroupRepository;
import ru.collegehub.backend.repository.RoleRepository;
import ru.collegehub.backend.repository.ScheduleRepository;
import ru.collegehub.backend.repository.SpecialityRepository;
import ru.collegehub.backend.repository.StudentRepository;
import ru.collegehub.backend.repository.SubjectRepository;
import ru.collegehub.backend.repository.UserRepository;
import ru.collegehub.backend.repository.UserRoleRepository;
import ru.collegehub.backend.util.PasswordGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SpecialityRepository specialityRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordGenerator passwordGenerator;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void createUser() {
        UserRequest userRequest = new UserRequest();
        userRequest.setFirstname("Ivan");
        userRequest.setLastname("Ivanov");
        userRequest.setEmail("ivan@gmail.com");
        userRequest.setRoles(List.of("ADMIN"));

        Role role = new Role();
        role.setName("ADMIN");

        when(passwordGenerator.generate()).thenReturn("1234");
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(role));

        MessageResponse response = adminService.createUser(userRequest);

        assertNotNull(response);
    }

    @Test
    void createSpeciality() {
        SpecialtyRequest specialtyRequest = new SpecialtyRequest();
        specialtyRequest.setId("09.04.02");
        specialtyRequest.setName("Anything");

        MessageResponse response = adminService.createSpeciality(specialtyRequest);

        assertNotNull(response);
        assertEquals("Speciality " + specialtyRequest.getId() + " was created", response.getMessage());
    }

    @Test
    void createGroupWithNameAndCourse() {
        GroupRequest request = new GroupRequest();
        request.setName("Group A");
        request.setCourse(1);

        MessageResponse response = adminService.createGroup(request);

        assertEquals("Group Group A was created", response.getMessage());
    }

    @Test
    void createSubject_createsSubjectAndSavesToDatabase() {
        SubjectRequest request = new SubjectRequest();
        request.setName("Math");

        MessageResponse response = adminService.createSubject(request);

        assertNotNull(response);
        assertEquals("Subject Math was created", response.getMessage());

    }

    @Test
    void addStudent_success() {
        Long studentId = 1L;
        String groupName = "Group A";
        Integer subgroup = 1;
        StudentDTO dto = new StudentDTO(studentId, groupName, subgroup);

        User user = new User();
        user.setId(studentId);
        Group group = new Group();
        group.setName(groupName);

        when(userRepository.findById(studentId)).thenReturn(Optional.of(user));
        when(groupRepository.findByNameIgnoreCase(groupName)).thenReturn(Optional.of(group));

        adminService.addStudent(dto);

        verify(userRepository).findById(studentId);
        verify(groupRepository).findByNameIgnoreCase(groupName);
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void getUsers() {
        Role role = new Role();
        role.setName("ADMIN");
        List<User> users = new ArrayList<>();
        UserRole user1 = new UserRole();
        UserRole user2 = new UserRole();
        users.add(new User(1L, "Ivan", "Ivanov", null, "ivan@gmail.com", "akjdkajkdad", List.of(user1)));
        users.add(new User(2L, "Petr", "Petrov", null, "petr@gmail.com", "akjdkajkdad", List.of(user2)));

        user1.setUser(users.get(0));
        user1.setRole(role);

        user2.setUser(users.get(1));
        user2.setRole(role);

        when(userRepository.findAllByQuery()).thenReturn(users);

        List<UserResponse> list = adminService.getUsers(null);

        assertNotNull(list);
        assertEquals(2, list.size());
    }

    @Test
    void getUserById() {
        Role role = new Role();
        role.setName("ADMIN");

        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .firstname("John")
                .lastname("Doe")
                .patronymic("Smith")
                .email("john.doe@example.com")
                .roles(List.of(UserRole.builder()
                        .role(role)
                        .build()))
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDetailsResponse result = adminService.getUserById(userId);

        assertEquals(userId, result.getId());
        assertEquals("John", result.getFirstname());
        assertEquals("Doe", result.getLastname());
        assertEquals("Smith", result.getPatronymic());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals(List.of("ADMIN"), result.getRoles());
    }

    @Test
    void getGroups() {
        List<Group> groups = new ArrayList<>();
        groups.add(new Group(1L, "Group 1", 1, null));
        groups.add(new Group(2L, "Group 2", 2, null));
        when(groupRepository.findAll()).thenReturn(groups);

        List<GroupResponse> result = adminService.getGroups();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Group 1", result.get(0).getName());
        assertEquals(2L, result.get(1).getId());
        assertEquals("Group 2", result.get(1).getName());
    }

    @Test
    void getGroupById() {
        Long groupId = 1L;
        Group group = new Group();
        group.setId(groupId);
        group.setName("Group 1");
        group.setCourse(1);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        GroupDetailsResponse result = adminService.getGroupById(groupId);

        assertEquals(groupId, result.getId());
        assertEquals("Group 1", result.getName());
        assertEquals(1, result.getCourse());
    }

    @Test
    void getSpecialities() {
        List<Speciality> specialities = new ArrayList<>();
        specialities.add(new Speciality("1", "Speciality 1", Collections.emptyList()));
        specialities.add(new Speciality("2", "Speciality 2", Collections.emptyList()));
        when(specialityRepository.findAll()).thenReturn(specialities);

        List<SpecialityResponse> result = adminService.getSpeciality();

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("Speciality 1", result.get(0).getName());
        assertEquals("2", result.get(1).getId());
        assertEquals("Speciality 2", result.get(1).getName());
    }

    @Test
    void getSpecialityById() {
        String id = "validId";
        Speciality speciality = new Speciality();
        speciality.setId(id);
        speciality.setName("Speciality Name");

        when(specialityRepository.findById(id)).thenReturn(Optional.of(speciality));

        SpecialityResponse result = adminService.getSpecialityById(id);

        assertEquals(id, result.getId());
        assertEquals("Speciality Name", result.getName());
    }

    @Test
    void test_createSchedule_validInputs() {
        ScheduleRequest request = new ScheduleRequest();
        request.setDayOfWeek("01.01.2022");
        request.setGroup(1L);
        request.setTeacher(1L);
        request.setSubject(1L);
        request.setNumberPair(1);
        request.setClassroom("A101");

        when(groupRepository.findById(1L)).thenReturn(Optional.of(new Group()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(new Subject()));

        MessageResponse response = adminService.createSchedule(request);

        assertNotNull(response);
        assertEquals("Schedule was added", response.getMessage());
    }

}