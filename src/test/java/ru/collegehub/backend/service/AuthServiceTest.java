package ru.collegehub.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.collegehub.backend.api.request.AuthRequest;
import ru.collegehub.backend.model.User;
import ru.collegehub.backend.security.JwtTokenUtil;
import ru.collegehub.backend.security.UserDetailsServiceImpl;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void successfulLogin_withValidCredentials() {
        var request = AuthRequest.builder()
                .username("valid")
                .password("valid")
                .build();

        var expectedTokens = Map.of("token", "valid_access_token",
                "refreshToken", "valid_refresh_Token");

        when(authenticationManager
                .authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        when(userDetailsService.loadUserByUsername(request.getUsername()))
                .thenReturn(User.builder()
                        .email(request.getUsername())
                        .build());

        when(jwtTokenUtil.generateToken(User.builder()
                .email(request.getUsername())
                .build())).thenReturn(expectedTokens.get("token"));

        when(jwtTokenUtil.generateRefreshToken(User.builder()
                .email(request.getUsername())
                .build())).thenReturn(expectedTokens.get("refreshToken"));

        var actualTokens = authService.login(request);

        assertEquals(expectedTokens, actualTokens);
    }

    @Test
    void refreshAccessToken_withValidRefreshToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_refresh_token");

        var user = User.builder()
                .email("valid")
                .build();

        when(jwtTokenUtil.getUsernameFromToken("valid_refresh_token")).thenReturn(user.getUsername());
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtTokenUtil.validateRefreshToken("valid_refresh_token", user)).thenReturn(true);
        when(jwtTokenUtil.generateToken(user)).thenReturn("new_access_token");
        when(jwtTokenUtil.generateRefreshToken(user)).thenReturn("new_refresh_token");

        Map<String, String> expectedTokens = Map.of("token", "new_access_token", "refreshToken", "new_refresh_token");

        Map<String, String> actualTokens = authService.refreshAndGetAuthenticationToken(request);

        assertEquals(expectedTokens, actualTokens);
    }

    @Test
    void failedLogin_withInvalidUsername() {
        var request = AuthRequest.builder()
                .username("")
                .password("valid")
                .build();

        when(authenticationManager
                .authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(UsernameNotFoundException.class);

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
    }

    @Test
    public void test_refresh_access_token_with_invalid_refresh_token() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_refresh_token");

        User user = User.builder()
                .email("valid")
                .build();
        when(jwtTokenUtil.getUsernameFromToken("invalid_refresh_token")).thenReturn(user.getUsername());
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtTokenUtil.validateRefreshToken("invalid_refresh_token", user)).thenReturn(false);

        Map<String, String> expectedTokens = Collections.emptyMap();

        Map<String, String> actualTokens = authService.refreshAndGetAuthenticationToken(request);

        assertEquals(expectedTokens, actualTokens);
    }
}