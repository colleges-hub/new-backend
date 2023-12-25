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

import java.util.Collections;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
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
        var userDetails = new User();
        userDetails.setRoles(Collections.emptyList());
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String mockAccessToken = "mockAccessToken";
        String mockRefreshToken = "mockRefreshToken";
        when(jwtTokenUtil.generateToken(any(User.class), any())).thenReturn(mockAccessToken);
        when(jwtTokenUtil.generateRefreshToken(any(User.class), any())).thenReturn(mockRefreshToken);

        AuthRequest testRequest = new AuthRequest();
        testRequest.setEmail("valid");
        testRequest.setPassword("valid");
        AuthResponse authResponse = authService.signin(testRequest);

        assertNotNull(authResponse);
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

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);


        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);


        when(jwtTokenUtil.generateToken(user, user.getAuthorities())).thenReturn("mockedAccessToken");
        when(jwtTokenUtil.generateRefreshToken(user, user.getAuthorities())).thenReturn("mockedRefreshToken");


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