package ru.collegehub.backend.controller;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.collegehub.backend.api.request.ClassroomRequest;
import ru.collegehub.backend.api.request.UserPatchRequest;
import ru.collegehub.backend.api.response.UserProfileResponse;
import ru.collegehub.backend.api.response.admin.MessageResponse;
import ru.collegehub.backend.service.UserService;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@RequestParam(name = "id", required = false) Long id) {
        return ResponseEntity.ok().body(userService.getProfile(id));
    }

    @PatchMapping("/update")
    public ResponseEntity<MessageResponse> updateUser(@RequestBody UserPatchRequest request) {
        return ResponseEntity.ok().body(userService.updateUser(request));
    }

    @GetMapping("/schedule")
    public ResponseEntity<?> getSchedule(@RequestParam(name = "id", required = false) @Min(1) Long id) {
        return ResponseEntity.ok().body(userService.getSchedule(id));
    }


    @PatchMapping("/update-classroom")
    @PreAuthorize("hasAuthority('TEACHER')")
    public ResponseEntity<MessageResponse> changeClassroom(@RequestBody ClassroomRequest request) {
        return ResponseEntity.ok().body(userService.changeClassroom(request));
    }
}
