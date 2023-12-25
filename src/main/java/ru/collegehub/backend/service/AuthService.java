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
import ru.collegehub.backend.security.JwtTokenUtil;
import ru.collegehub.backend.security.UserDetailsImpl;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    @Transactional(readOnly = true)
    public AuthResponse signin(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = (UserDetailsImpl) userDetailsService.loadUserByUsername(request.getEmail());

        var accessToken = jwtTokenUtil.generateToken(user.getUser(), user.getAuthorities());
        var refreshToken = jwtTokenUtil.generateRefreshToken(user.getUser(), user.getAuthorities());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    public AuthResponse refreshToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserDetailsImpl) authentication.getPrincipal();

        var accessToken = jwtTokenUtil.generateToken(principal.getUser(), principal.getAuthorities());
        var newRefreshToken = jwtTokenUtil.generateRefreshToken(principal.getUser(), principal.getAuthorities());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
