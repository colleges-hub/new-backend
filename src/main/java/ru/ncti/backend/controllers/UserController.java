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
import org.springframework.web.bind.annotation.RestController;
import ru.ncti.backend.dto.ChangePasswordDTO;
import ru.ncti.backend.dto.FcmDTO;
import ru.ncti.backend.dto.GroupViewDTO;
import ru.ncti.backend.dto.ScheduleDTO;
import ru.ncti.backend.dto.UserViewDTO;
import ru.ncti.backend.service.UserService;

import javax.websocket.server.PathParam;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 27-05-2023
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PatchMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(userService.changePassword(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<String> getFCMToken(@RequestBody FcmDTO dto) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.addFCMToken(dto));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupViewDTO>> getAllGroups() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<Map<String, Set<ScheduleDTO>>> getScheduleByGroupId(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getSchedule(id));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserViewDTO>> getUsers(@PathParam("type") String type) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUsers(type));
    }
}
