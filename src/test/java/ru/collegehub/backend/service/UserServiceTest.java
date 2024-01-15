package ru.collegehub.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.collegehub.backend.api.request.ClassroomRequest;
import ru.collegehub.backend.api.request.UserPatchRequest;
import ru.collegehub.backend.api.response.ScheduleResponse;
import ru.collegehub.backend.api.response.admin.MessageResponse;
import ru.collegehub.backend.exception.UserNotFoundException;
import ru.collegehub.backend.model.Group;
import ru.collegehub.backend.model.Role;
import ru.collegehub.backend.model.Schedule;
import ru.collegehub.backend.model.Student;
import ru.collegehub.backend.model.Subject;
import ru.collegehub.backend.model.User;
import ru.collegehub.backend.model.UserRole;
import ru.collegehub.backend.repository.GroupRepository;
import ru.collegehub.backend.repository.ScheduleRepository;
import ru.collegehub.backend.repository.StudentRepository;
import ru.collegehub.backend.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private UserService userService;

    @Test
    void getProfile_withValidId() {
        var user = createUser();

        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(user));

        var result = userService.getProfile(1L);

        assertNotNull(result);
        assertEquals(result.getFirstname(), "Ivan");
        assertEquals(result.getLastname(), "Ivanov");
    }

    @Test
    void getProfile_withInvalidId() {
        when(userRepository.findByIdWithRoles(anyLong())).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class, () -> userService.getProfile(1L)
        );
    }

    @Test
    void getProfile_withSecurityContext() {
        var user = createUser();

        var authentication = mock(Authentication.class);
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(user);


        var profile = userService.getProfile(null);

        assertNotNull(profile);
        assertEquals(profile.getFirstname(), "Ivan");
        assertEquals(profile.getLastname(), "Ivanov");
    }

    @Test
    void updateUser_ShouldReturnMessageResponse() {
        var authentication = mock(Authentication.class);

        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        var user = new User();
        user.setId(1L);

        when(authentication.getPrincipal()).thenReturn(user);

        when(userRepository.save(user)).thenReturn(user);

        UserPatchRequest patchRequest = new UserPatchRequest();
        patchRequest.setFirstname("NewFirstName");
        patchRequest.setLastname("NewLastName");

        MessageResponse messageResponse = userService.updateUser(patchRequest);

        verify(userRepository, times(1)).save(user);

        assertEquals("User data was update", messageResponse.getMessage());
    }

    @Test
    void getSchedule_WithGroupId_ShouldReturnMap() {
        Long groupId = 1L;

        var user = createUser();
        var authentication = mock(Authentication.class);
        var securityContext = mock(SecurityContext.class);

        when(authentication.getPrincipal()).thenReturn(user);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Group group = createGroup();
        List<Schedule> scheduleList = createScheduleList();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(scheduleRepository.findScheduleByGroupOnCurrentWeek(group)).thenReturn(scheduleList);

        Map<String, List<ScheduleResponse>> result = userService.getSchedule(groupId);

        assertFalse(result.isEmpty());
    }

    @Test
    void getSchedule_WithoutGroupIdAsStudent_ShouldReturnMap() {
        var user = createUserWithStudentRole();
        var authentication = mock(Authentication.class);
        var securityContext = mock(SecurityContext.class);

        when(authentication.getPrincipal()).thenReturn(user);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        var group = createGroup();
        List<Schedule> scheduleList = createScheduleList();
        var student = createStudent();
        when(studentRepository.findByStudentWithGroup(user)).thenReturn(Optional.of(student));
        lenient().when(groupRepository.findById(student.getGroup().getId())).thenReturn(Optional.of(group));
        when(scheduleRepository.findScheduleByGroupOnCurrentWeek(student.getGroup())).thenReturn(scheduleList);

        Map<String, List<ScheduleResponse>> result = userService.getSchedule(null);

        assertFalse(result.isEmpty());
    }

    @Test
    void getSchedule_WithoutGroupIdAsTeacher_ShouldReturnMap() {
        var user = createUser();
        var authentication = mock(Authentication.class);
        var securityContext = mock(SecurityContext.class);

        when(authentication.getPrincipal()).thenReturn(user);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        List<Schedule> scheduleList = createScheduleList();
        when(scheduleRepository.findScheduleByTeacherOnCurrentWeek(user)).thenReturn(scheduleList);

        Map<String, List<ScheduleResponse>> result = userService.getSchedule(null);

        assertFalse(result.isEmpty());
    }

    @Test
    void testChangeClassroom() {
        // Arrange
        Long scheduleId = 1L;
        String newClassroom = "NewClassroom";

        ClassroomRequest request = new ClassroomRequest();
        request.setId(scheduleId);
        request.setClassroom(newClassroom);

        Schedule existingSchedule = new Schedule();
        existingSchedule.setId(scheduleId);
        existingSchedule.setClassroom("OldClassroom");

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(existingSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MessageResponse response = userService.changeClassroom(request);

        // Assert
        verify(scheduleRepository, times(1)).findById(scheduleId);
        verify(scheduleRepository, times(1)).save(any(Schedule.class));

        assertEquals("Classroom was updated on " + newClassroom, response.getMessage());
        assertEquals(newClassroom, existingSchedule.getClassroom());
    }

    private User createUser() {
        var role = new Role();
        role.setName("TEACHER");
        var user = new User(1L,
                "Ivan",
                "Ivanov", null,
                "ivan@gmail.com", "pass", new ArrayList<>());

        var userRole = new UserRole();
        userRole.setRole(role);
        userRole.setUser(user);
        user.getRoles().add(userRole);

        return user;
    }

    private User createUserWithStudentRole() {
        var role = new Role();
        role.setName("STUDENT");
        var user = new User(2L,
                "Ivan",
                "Ivanov", null,
                "ivan@gmail.com", "pass", new ArrayList<>());

        var userRole = new UserRole();
        userRole.setRole(role);
        userRole.setUser(user);
        user.getRoles().add(userRole);

        return user;
    }

    private Student createStudent() {
        return new Student(1L, createUserWithStudentRole(), createGroup(), 1);
    }

    private Group createGroup() {
        return new Group(1L, "A0101", 1, null);
    }

    private List<Schedule> createScheduleList() {
        List<Schedule> schedules = new ArrayList<>();

        Group group = createGroup();
        User teacher = createUser();
        var subject = createSubject();

        Schedule schedule1 = new Schedule(1L, LocalDate.now(), group, teacher, 1, subject, "Classroom1");
        Schedule schedule2 = new Schedule(2L, LocalDate.now(), group, teacher, 2, subject, "Classroom2");

        schedules.add(schedule1);
        schedules.add(schedule2);

        return schedules;
    }

    public Subject createSubject() {
        return new Subject(1L, "Math");
    }
}