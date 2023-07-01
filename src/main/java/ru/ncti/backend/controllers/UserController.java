package ru.ncti.backend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import ru.ncti.backend.api.request.FCMRequest;
import ru.ncti.backend.api.response.GroupResponse;
import ru.ncti.backend.api.response.ScheduleResponse;
import ru.ncti.backend.api.response.UserResponse;
import ru.ncti.backend.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * user: ichuvilin
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PatchMapping("/update-credential")
    public ResponseEntity<String> updateCredential(@RequestBody AuthRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(userService.updateCredential(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<String> getFCMToken(@RequestBody FCMRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.addFCMToken(request));
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFileToMinIO(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUserPhoto(file));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupResponse>> getAllGroups() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<Map<String, Set<ScheduleResponse>>> getScheduleByGroupId(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getSchedule(id));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserById(id));
    }
}
