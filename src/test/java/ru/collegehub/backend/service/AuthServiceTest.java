package ru.collegehub.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.collegehub.backend.api.request.AuthRequest;
import ru.collegehub.backend.api.response.AuthResponse;
import ru.collegehub.backend.model.User;
import ru.collegehub.backend.security.JwtTokenUtil;
import ru.collegehub.backend.security.UserDetailsImpl;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserDetailsService userDetailsService;


    @InjectMocks
    private AuthService authService;


    @Test
    void successfulSignin_withValidCredentials() {
        var request = new AuthRequest();
        request.setEmail("valid");
        request.setPassword("valid");

        var user = new User();
        user.setEmail("valid");
        user.setRoles(Collections.emptyList());

        var userDetails = new UserDetailsImpl(user);

        var expectedTokens = Map.of("token", "valid_access_token",
                "refreshToken", "valid_refresh_Token");

        when(authenticationManager
                .authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        when(userDetailsService.loadUserByUsername(request.getEmail()))
                .thenReturn(userDetails);

        when(jwtTokenUtil.generateToken(userDetails.getUser(), userDetails.getAuthorities()))
                .thenReturn(expectedTokens.get("token"));

        when(jwtTokenUtil.generateRefreshToken(userDetails.getUser(), userDetails.getAuthorities()))
                .thenReturn(expectedTokens.get("refreshToken"));

        var actualTokens = authService.signin(request);

        assertEquals(expectedTokens.get("token"), actualTokens.getToken());
        assertEquals(expectedTokens.get("refreshToken"), actualTokens.getRefreshToken());
    }

    @Test
    void failedLogin_withInvalidCredentials() {
        var request = new AuthRequest();
        when(authenticationManager
                .authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(UsernameNotFoundException.class);

        assertThrows(UsernameNotFoundException.class, () -> authService.signin(request));
    }

    @Test
    void successfulRefreshToken_withValidToken() {
        var user = new User();
        user.setEmail("admin@admim.com");
        user.setRoles(Collections.emptyList());
        UserDetailsImpl userDetails = new UserDetailsImpl(user);


        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);


        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);


        when(jwtTokenUtil.generateToken(userDetails.getUser(), userDetails.getAuthorities())).thenReturn("mockedAccessToken");
        when(jwtTokenUtil.generateRefreshToken(userDetails.getUser(), userDetails.getAuthorities())).thenReturn("mockedRefreshToken");


        AuthResponse result = authService.refreshToken();

        assertEquals("mockedAccessToken", result.getToken());
        assertEquals("mockedRefreshToken", result.getRefreshToken());
    }

    @Test
    void refreshToken_ThrowsExceptionWhenAuthenticationIsNull() {
        SecurityContextHolder.clearContext();

        assertThrows(NullPointerException.class, () -> authService.refreshToken());
    }

}