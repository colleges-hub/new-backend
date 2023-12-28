package ru.collegehub.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
import ru.collegehub.backend.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/admin")
@Validated
@AllArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/create-user")
    public ResponseEntity<MessageResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok().body(adminService.createUser(request));
    }

    @PostMapping("/create-speciality")
    public ResponseEntity<MessageResponse> createSpeciality(@Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.ok().body(adminService.createSpeciality(request));
    }

    @PostMapping("/create-group")
    public ResponseEntity<MessageResponse> createGroup(@Valid @RequestBody GroupRequest request) {
        return ResponseEntity.ok().body(adminService.createGroup(request));
    }

    @PostMapping("/create-subject")
    public ResponseEntity<MessageResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok().body(adminService.createSubject(request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers(@RequestParam(value = "type", required = false) String type) {
        return ResponseEntity.ok().body(adminService.getUsers(type));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDetailsResponse> getUserById(@PathVariable(name = "id") @Min(value = 1) Long id) {
        return ResponseEntity.ok().body(adminService.getUserById(id));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupResponse>> getGroups() {
        return ResponseEntity.ok().body(adminService.getGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupDetailsResponse> getGroupById(@PathVariable(name = "id") @Min(value = 1) Long id) {
        return ResponseEntity.ok().body(adminService.getGroupById(id));
    }

    @GetMapping("/specialities")
    public ResponseEntity<List<SpecialityResponse>> getSpecialities() {
        return ResponseEntity.ok().body(adminService.getSpeciality());
    }

    @GetMapping("/specialities/{id}")
    public ResponseEntity<SpecialityResponse> getSpecialityById(@PathVariable(name = "id") String id) {
        return ResponseEntity.ok().body(adminService.getSpecialityById(id));
    }
}
