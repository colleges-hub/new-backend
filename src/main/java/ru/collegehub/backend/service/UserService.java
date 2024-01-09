package ru.collegehub.backend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.internal.util.CopyOnWriteLinkedHashMap;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.collegehub.backend.api.request.UserPatchRequest;
import ru.collegehub.backend.api.response.ScheduleResponse;
import ru.collegehub.backend.api.response.UserProfileResponse;
import ru.collegehub.backend.api.response.admin.MessageResponse;
import ru.collegehub.backend.exception.GroupNotFoundException;
import ru.collegehub.backend.exception.StudentNotFoundException;
import ru.collegehub.backend.exception.UserNotFoundException;
import ru.collegehub.backend.model.Schedule;
import ru.collegehub.backend.model.User;
import ru.collegehub.backend.repository.GroupRepository;
import ru.collegehub.backend.repository.ScheduleRepository;
import ru.collegehub.backend.repository.StudentRepository;
import ru.collegehub.backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long id) {
        if (id != null) {
            User user = userRepository.findByIdWithRoles(id).orElseThrow(
                    () -> new UserNotFoundException("User " + id + " not found")
            );

            return getUserProfileResponse(user);
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (User) authentication.getPrincipal();

        return getUserProfileResponse(principal);
    }

    @Transactional
    public MessageResponse updateUser(UserPatchRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (User) authentication.getPrincipal();

        BeanUtils.copyProperties(request, principal, getNullPropertyNames(request));

        userRepository.save(principal);

        return new MessageResponse("User data was update");
    }


    @Transactional(readOnly = true)
    public Map<String, List<ScheduleResponse>> getSchedule(Long groupId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var user = (User) authentication.getPrincipal();

        Map<String, List<ScheduleResponse>> map = new CopyOnWriteLinkedHashMap<>();

        if (groupId != null) {
            generateScheduleForGroup(groupId, map);
        } else if (user.getRoles().stream().allMatch(role -> role.getRole().getName().equalsIgnoreCase("student"))) {
            generateScheduleForStudent(user, map);
        } else if (user.getRoles().stream().allMatch(role -> role.getRole().getName().equalsIgnoreCase("teacher"))) {
            generateScheduleForTeacher(user, map);
        }
        map.values().parallelStream().forEach(scheduleResponses -> scheduleResponses.sort(Comparator.comparingInt(ScheduleResponse::getNumberPair)));
        return map;
    }

    private String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        log.info("{}", emptyNames);
        return emptyNames.toArray(new String[0]);
    }

    private UserProfileResponse getUserProfileResponse(User user) {
        List<String> roles = user.getRoles().stream().map(
                role -> role.getRole().getDescription() != null ?
                        role.getRole().getDescription() :
                        role.getRole().getName()
        ).toList();

        return UserProfileResponse.builder()
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .patronymic(user.getPatronymic())
                .roles(roles)
                .build();
    }

    private void generateScheduleForGroup(Long groupId, Map<String, List<ScheduleResponse>> map) {
        var group = groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException("Group not found"));
        var scheduleByGroupOnCurrentWeek = scheduleRepository.findScheduleByGroupOnCurrentWeek(group);
        addToScheduleMap(scheduleByGroupOnCurrentWeek, map, false);
    }

    private void generateScheduleForStudent(User user, Map<String, List<ScheduleResponse>> map) {
        var student = studentRepository.findByStudentWithGroup(user).orElseThrow(() -> new StudentNotFoundException("Student not found"));
        var scheduleByGroupOnCurrentWeek = scheduleRepository.findScheduleByGroupOnCurrentWeek(student.getGroup());
        addToScheduleMap(scheduleByGroupOnCurrentWeek, map, false);
    }

    private void generateScheduleForTeacher(User user, Map<String, List<ScheduleResponse>> map) {
        var scheduleByTeacherOnCurrentWeek = scheduleRepository.findScheduleByTeacherOnCurrentWeek(user);
        addToScheduleMap(scheduleByTeacherOnCurrentWeek, map, true);
    }

    private void addToScheduleMap(List<Schedule> scheduleList, Map<String, List<ScheduleResponse>> map, boolean isTeacher) {
        scheduleList.forEach(schedule -> {
            map.computeIfAbsent(schedule.getDayOfWeek().toString(), list -> new ArrayList<>(5))
                    .add(createScheduleResponse(schedule, isTeacher));
        });
    }

    private ScheduleResponse createScheduleResponse(Schedule schedule, boolean isTeacher) {
        ScheduleResponse.ScheduleResponseBuilder builder = ScheduleResponse.builder()
                .numberPair(schedule.getNumberPair())
                .subject(schedule.getSubject().getName())
                .classroom(schedule.getClassroom());

        if (isTeacher) {
            builder.payload(List.of(schedule.getGroup().getName()));
        } else {
            builder.payload(List.of(String.format("%s %s", schedule.getTeacher().getFirstname(), schedule.getTeacher().getLastname())));
        }

        return builder.build();
    }
}
