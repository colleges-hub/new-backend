package ru.collegehub.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.collegehub.backend.api.request.AuthRequest;
import ru.collegehub.backend.model.User;
import ru.collegehub.backend.security.JwtTokenUtil;
import ru.collegehub.backend.security.UserDetailsServiceImpl;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    public Map<String, String> login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User userDetails = (User) userDetailsService.loadUserByUsername(request.getUsername());

        String accessToken = jwtTokenUtil.generateToken(userDetails);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        return Map.of("token", accessToken, "refreshToken", refreshToken);
    }

    public Map<String, String> refreshAndGetAuthenticationToken(HttpServletRequest request) {
        String authToken = request.getHeader("Authorization");
        final String token = authToken.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        User userDetails = (User) userDetailsService.loadUserByUsername(username);

        if (jwtTokenUtil.validateRefreshToken(token, userDetails)) {
            String accessToken = jwtTokenUtil.generateToken(userDetails);
            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

            return Map.of("token", accessToken, "refreshToken", refreshToken);
        }
        return Collections.emptyMap();
    }
}
