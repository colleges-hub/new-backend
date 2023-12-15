package ru.collegehub.backend.controllers;

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
import ru.collegehub.backend.api.request.AuthRequest;
import ru.collegehub.backend.api.response.GroupResponse;
import ru.collegehub.backend.api.response.ScheduleResponse;
import ru.collegehub.backend.api.response.UserResponse;
import ru.collegehub.backend.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Set<ScheduleResponse>>> getSchedule() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getSchedule());
    }

    @PatchMapping("/update-credential")
    public ResponseEntity<String> updateCredential(@RequestBody AuthRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(userService.updateCredential(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupResponse>> getGroups() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<Map<String, Set<ScheduleResponse>>> getScheduleByGroupId(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getSchedule(id));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "120") int size) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUsersWithPagination(page, size));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserById(id));
    }

    @PostMapping("/order-certificate")
    public ResponseEntity<?> orderCertificate() {
        return ResponseEntity.status(HttpStatus.OK).body("QAQQ");
    }
}
