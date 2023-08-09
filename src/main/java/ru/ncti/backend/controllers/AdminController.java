package ru.ncti.backend.controllers;

import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.ncti.backend.api.request.AuthRequest;
import ru.ncti.backend.api.request.GroupRequest;
import ru.ncti.backend.api.request.ScheduleRequest;
import ru.ncti.backend.api.request.SectionRequest;
import ru.ncti.backend.api.request.SpecialityRequest;
import ru.ncti.backend.api.request.SubjectRequest;
import ru.ncti.backend.api.request.TemplateRequest;
import ru.ncti.backend.api.request.UserRequest;
import ru.ncti.backend.api.response.GroupResponse;
import ru.ncti.backend.api.response.ScheduleResponse;
import ru.ncti.backend.api.response.SectionResponse;
import ru.ncti.backend.api.response.SpecialityResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.model.Group;
import ru.ncti.backend.model.Subject;
import ru.ncti.backend.service.AdminService;

import java.io.IOException;
import java.util.List;

/**
 * user: ichuvilin
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getProfile());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsersByType(@RequestParam("type") String type) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getUsersByType(type));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getUserById(id));
    }

    @PostMapping("/users")
    public ResponseEntity<String> createUser(@RequestBody UserRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(dto));
    }

    @PostMapping("/upload-users")
    public ResponseEntity<String> uploadUsers(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.uploadUsers(file));
        } catch (IOException | CsvValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getRoles());
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupResponse>> getGroups() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getGroupById(id));
    }

    @PostMapping("/subject")
    public ResponseEntity<String> createSubject(@RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.createSubject(request));
    }


    @PostMapping("/upload-subjects")
    public ResponseEntity<String> uploadSubjects(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.uploadSubjects(file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> getSubjects() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getSubjects());
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<Subject> getSubjectById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getSubjectById(id));
    }

    @PostMapping("/speciality")
    public ResponseEntity<String> createSpeciality(@RequestBody SpecialityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createSpeciality(request));
    }

    @PostMapping("/upload-specialities")
    public ResponseEntity<?> uploadSpeciality(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminService.uploadSpeciality(file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/specialities")
    public ResponseEntity<List<SpecialityResponse>> getSpecialities() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getSpecialities());
    }

    @GetMapping("/sections")
    public ResponseEntity<List<SectionResponse>> getSections() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getSections());
    }

    @PostMapping("/sections")
    public ResponseEntity<String> createSection(@RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.createSection(request));
    }

    @PostMapping("/group")
    public ResponseEntity<String> createGroup(@RequestBody GroupRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createGroup(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/create-template")
    public ResponseEntity<String> createTemplate(@RequestBody TemplateRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.createTemplate(request));
    }

    @PostMapping("/change-schedule")
    public ResponseEntity<?> changeSchedule(@RequestBody ScheduleRequest dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.changeSchedule(dto));
    }


    // todo: all rework
    @PatchMapping("/update")
    public ResponseEntity<String> updateProfile(@RequestBody AuthRequest dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.updateProfile(dto));
    }

    @PostMapping("/upload-schedule")
    public ResponseEntity<String> uploadTemplate(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.uploadTemplate(file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/students")
    public ResponseEntity<List<UserResponse>> getStudents(@RequestParam("group") Long group) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getStudentsByGroup(group));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteStudentById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteUserById(id));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<String> deleteGroupById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteGroupById(id));
    }

    @PatchMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestParam("id") Long id, @RequestBody AuthRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.updateCredentialUserById(id, request));
    }

}
