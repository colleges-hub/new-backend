package ru.ncti.backend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ncti.backend.api.response.ScheduleResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.service.StudentService;

import java.util.Map;
import java.util.Set;

/**
 * user: ichuvilin
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.status(HttpStatus.OK).body(studentService.getProfile());
    }

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Set<ScheduleResponse>>> getSchedule() {
        return ResponseEntity.status(HttpStatus.OK).body(studentService.schedule());
    }

    @PostMapping("/certificate")
    public void orderCertificate() {
        // todo
    }
}
