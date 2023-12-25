package ru.collegehub.backend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.collegehub.backend.api.request.AuthRequest;
import ru.collegehub.backend.api.response.AuthResponse;
import ru.collegehub.backend.model.User;
import ru.collegehub.backend.security.JwtTokenUtil;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    @Transactional(readOnly = true)
    public AuthResponse signin(AuthRequest request) {
        var authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var principal = (User) authenticate.getPrincipal();

        var accessToken = jwtTokenUtil.generateToken(principal, principal.getAuthorities());
        var refreshToken = jwtTokenUtil.generateRefreshToken(principal, principal.getAuthorities());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    public AuthResponse refreshToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (User) authentication.getPrincipal();

        var accessToken = jwtTokenUtil.generateToken(principal, principal.getAuthorities());
        var newRefreshToken = jwtTokenUtil.generateRefreshToken(principal, principal.getAuthorities());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
