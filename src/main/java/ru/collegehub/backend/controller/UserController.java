package ru.collegehub.backend.controller;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.collegehub.backend.api.request.UserPatchRequest;
import ru.collegehub.backend.api.response.UserProfileResponse;
import ru.collegehub.backend.api.response.admin.MessageResponse;
import ru.collegehub.backend.service.UserService;


// todo: add tests
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


    // todo: change schedule for teacher
}
