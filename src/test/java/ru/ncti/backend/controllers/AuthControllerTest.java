package ru.ncti.backend.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.ncti.backend.api.request.AuthRequest;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String AUTH_PATH = "/auth";

    private static AuthRequest REQUEST = AuthRequest.builder()
            .username("admin@gmail.com")
            .password("admin")
            .build();

    private static final ObjectMapper JSON_REQUEST = new ObjectMapper();

    @Test
    void shouldLogin() throws Exception {
        this.mockMvc.perform(post(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_REQUEST.writeValueAsString(REQUEST)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldNotLogin() throws Exception {
        REQUEST.setPassword("password");
        this.mockMvc.perform(post(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_REQUEST.writeValueAsString(REQUEST)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRefreshToken() throws Exception {
        MvcResult result = mockMvc.perform(post(AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_REQUEST.writeValueAsString(REQUEST)))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> data = objectMapper
                .readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        mockMvc.perform(post(AUTH_PATH + "/refresh")
                        .header("Authorization", "Bearer " + data.get("refreshToken")))
                .andDo(print())
                .andExpect(status().isOk());
    }

}