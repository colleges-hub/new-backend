package ru.collegehub.backend.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void successfulLogin() throws Exception {
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "admin@admin.com",
                                          "password": "admin"
                                        }
                                        """)
                )
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.token").isNotEmpty(),
                        jsonPath("$.refreshToken").isNotEmpty()
                );
    }

    @Test
    void failedLogin_withoutUsername() throws Exception {
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "",
                                          "password": "admin"
                                        }
                                        """)
                )
                .andExpectAll(
                        status().is4xxClientError(),
                        jsonPath("$.violations").exists(),
                        jsonPath("$.violations[0].fieldName").value("username"),
                        jsonPath("$.violations[0].message").isNotEmpty()
                );
    }

    @Test
    void failedLogin_withoutPassword() throws Exception {
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "admin@admin.com",
                                            "password": ""
                                        }
                                        """)
                )
                .andExpectAll(
                        status().is4xxClientError(),
                        jsonPath("$.violations").exists(),
                        jsonPath("$.violations[0].fieldName").value("password"),
                        jsonPath("$.violations[0].message").isNotEmpty()
                );
    }

}