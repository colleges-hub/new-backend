package ru.collegehub.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.collegehub.backend.api.request.AuthRequest;
import ru.collegehub.backend.api.response.AuthResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void successfulSignin_withValidCredentials() throws Exception {
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin@admin.com",
                                    "password": "admin"
                                }
                                """))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.token").isNotEmpty(),
                        jsonPath("$.refreshToken").isNotEmpty()
                );
    }

    @Test
    void failedSignin_withoutEmail() throws Exception {
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "",
                                    "password": "admin"
                                }
                                """))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.email").value("must not be blank")
                );
    }


    @Test
    void failedSignin_withoutPassword() throws Exception {
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin@admin.com",
                                    "password": ""
                                }
                                """))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.password").value("must not be blank")
                );
    }

    @Test
    void failedSignin_withInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin",
                                    "password": "password"
                                }
                                """))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.email").value("must be a well-formed email address")
                );
    }

    @Test
    void successfulRefreshToken_withValidToken() throws Exception {
        var request = new AuthRequest();

        request.setEmail("admin@admin.com");
        request.setPassword("admin");

        var url = "http://localhost:" + port + "/auth/signin";
        var response = restTemplate.postForEntity(url, request, AuthResponse.class);

        if (response.getBody() == null) {
            throw new Exception("Response body is null");
        }

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer " + response.getBody().getRefreshToken()))
                .andExpectAll(status().isOk(),
                        jsonPath("$.token").isNotEmpty(),
                        jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void failedRefreshToken_withoutToken() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpectAll(status().is(403));
    }
}