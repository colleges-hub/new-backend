package ru.ncti.backend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ncti.backend.dto.AuthDTO;
import ru.ncti.backend.dto.FcmDTO;
import ru.ncti.backend.service.AuthService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 26-05-2023
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AuthDTO dto) {
        return ResponseEntity.status(OK).body(authService.login(dto));
    }

    @PostMapping("/logout")
    public void logout(@RequestBody FcmDTO fcmDTO) {
            authService.logout(fcmDTO);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAndGetAuthenticationToken(HttpServletRequest request) {
        return ResponseEntity.status(OK).body(authService.refreshAndGetAuthenticationToken(request));
    }

}
