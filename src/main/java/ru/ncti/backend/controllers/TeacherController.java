package ru.ncti.backend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ncti.backend.dto.TeacherScheduleViewDTO;
import ru.ncti.backend.dto.TeacherViewDTO;
import ru.ncti.backend.service.TeacherService;

import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 27-05-2023
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/profile")
    public ResponseEntity<TeacherViewDTO> getProfile() {
        return ResponseEntity.status(HttpStatus.OK).body(teacherService.getProfile());
    }

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Set<TeacherScheduleViewDTO>>> getSchedule() {
        return ResponseEntity.status(HttpStatus.OK).body(teacherService.getSchedule());
    }
}
