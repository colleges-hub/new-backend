package ru.ncti.backend.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ncti.backend.api.request.AuthRequest;
import ru.ncti.backend.service.AuthService;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AuthRequest request) {
        return ResponseEntity.status(OK).body(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAndGetAuthenticationToken(HttpServletRequest request) {
        return ResponseEntity.status(OK).body(authService.refreshAndGetAuthenticationToken(request));
    }

}
