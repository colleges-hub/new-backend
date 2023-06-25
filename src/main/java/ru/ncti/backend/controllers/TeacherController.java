package ru.ncti.backend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.api.request.ScheduleChangeRequest;
import ru.ncti.backend.api.response.TeacherScheduleResponse;
import ru.ncti.backend.service.TeacherService;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

/**
 * user: ichuvilin
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.status(HttpStatus.OK).body(teacherService.getProfile());
    }

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Set<TeacherScheduleResponse>>> getSchedule() {
        return ResponseEntity.status(HttpStatus.OK).body(teacherService.schedule());
    }

    @PostMapping("/change-schedule")
    public ResponseEntity<String> changeSchedule(@RequestBody ScheduleChangeRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(teacherService.changeSchedule(request));
        } catch (ParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('DEANERY')")
    @GetMapping("/certificate")
    public ResponseEntity<String> getCertificateList() {
        return ResponseEntity.status(HttpStatus.OK).body("QQ");
    }

}
