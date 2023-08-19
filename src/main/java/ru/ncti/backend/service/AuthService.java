package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.ncti.backend.api.request.AuthRequest;
import ru.ncti.backend.model.User;
import ru.ncti.backend.repository.UserRepository;
import ru.ncti.backend.security.JwtTokenUtil;
import ru.ncti.backend.security.UserDetailsServiceImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * user: ichuvilin
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;

    public Map<String, String> login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

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

    public void logout() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        currentUser.setDevice(null);
        userRepository.save(currentUser);
    }
}
