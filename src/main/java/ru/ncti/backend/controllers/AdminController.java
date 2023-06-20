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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.ncti.backend.dto.GroupDTO;
import ru.ncti.backend.dto.ResetPasswordDTO;
import ru.ncti.backend.dto.SampleDTO;
import ru.ncti.backend.dto.ScheduleDTO;
import ru.ncti.backend.dto.SpecialityDTO;
import ru.ncti.backend.dto.StudentDTO;
import ru.ncti.backend.dto.SubjectDTO;
import ru.ncti.backend.dto.UserDTO;
import ru.ncti.backend.entity.Group;
import ru.ncti.backend.entity.Subject;
import ru.ncti.backend.entity.User;
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
    public ResponseEntity<UserDTO> getProfile() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getProfile());
    }

    @PatchMapping("/update")
    public ResponseEntity<UserDTO> updateProfile(@RequestBody UserDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.updateProfile(dto));
    }

    @PostMapping("/create-student")
    public ResponseEntity<String> createStudent(@RequestBody StudentDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createStudent(dto));
    }

    @PostMapping("/create-teacher")
    public ResponseEntity<String> createTeacher(@RequestBody UserDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTeacher(dto));
    }

    @PostMapping("/create-speciality")
    public ResponseEntity<String> createSpeciality(@RequestBody SpecialityDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createSpeciality(dto));
    }

    @PostMapping("/schedule")
    public ResponseEntity<String> createSchedule(@RequestBody SampleDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.createSchedule(dto));
    }

    @PostMapping("/create-group")
    public ResponseEntity<String> createGroup(@RequestBody GroupDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createGroup(dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/create-sample")
    public ResponseEntity<String> createSample(@RequestBody SampleDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.createSample(dto));
    }

    @PostMapping("/create-subject")
    public ResponseEntity<String> createSubject(@RequestBody SubjectDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.createSubject(dto));
    }

    @PostMapping("/upload-students")
    public ResponseEntity<String> uploadStudents(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.uploadStudents(file));
        } catch (IOException | CsvValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/upload-teachers")
    public ResponseEntity<String> uploadTeacher(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.uploadTeacher(file));
        } catch (IOException | CsvValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/upload-schedule")
    public ResponseEntity<String> uploadSchedule(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.uploadSchedule(file));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/change-schedule")
    public ResponseEntity<?> changeSchedule(@RequestBody ScheduleDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.changeSchedule(dto));
    }

    @GetMapping("/students")
    public ResponseEntity<List<User>> getStudents(@RequestParam("group") Long group) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getStudentsByGroup(group));
    }

    @GetMapping("/teachers")
    public ResponseEntity<List<User>> getTeachers() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getTeachers());
    }

    @GetMapping("/groups")
    public ResponseEntity<List<Group>> getGroups() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getGroups());
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<User> getStudentById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getUserById(id));
    }

    @GetMapping("/teachers/{id}")
    public ResponseEntity<User> getTeacherById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getUserById(id));
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getGroupById(id));
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> getSubjects() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getSubjects());
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<Subject> getSubjectById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getSubjectById(id));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<String> deleteStudentById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteUserById(id));
    }

    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<String> deleteTeacherById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteUserById(id));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<String> deleteGroupById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteGroupById(id));
    }

    @PutMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.resetPasswordForUserById(dto));
    }

}
