package ru.ncti.backend.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin@gmail.com")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ADMIN_PATH = "/admin";

    @Test
    void getProfile() throws Exception {
        this.mockMvc.perform(get(ADMIN_PATH + "/profile"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void updateProfile() throws Exception {
        String request = "{\"password\": \"admin\"}";

        this.mockMvc.perform(patch(ADMIN_PATH + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                )
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreateTeacher() throws Exception {
        String request = "{" +
                "\"firstname\": \"Иван\"," +
                "\"lastname\": \"Иванов\"," +
                "\"surname\": \"Иванович\"," +
                "\"email\": \"iv21an45@gmail.com\"," +
                "\"role\": [\"Преподаватель\"]," +
                "\"password\": \"password\"" +
                "}";

        this.mockMvc.perform(post(ADMIN_PATH + "/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    void shouldCreateStudentAndGroup() throws Exception {
        String groupRequest = "{" +
                "\"name\":\"A-1\"," +
                "\"course\":\"4\"" +
                "}";

        mockMvc.perform(post(ADMIN_PATH + "/create-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupRequest))
                .andDo(print())
                .andExpect(status().isCreated());

        String studentRequest = "{" +
                "\"firstname\": \"Иван\"," +
                "\"lastname\": \"Иванов\"," +
                "\"surname\": \"Иванович\"," +
                "\"email\": \"student@gmail.com\"," +
                "\"group\": \"A-1\"," +
                "\"role\": [\"Студент\"]," +
                "\"password\": \"password\"" +
                "}";
        System.out.println(studentRequest);
        this.mockMvc.perform(post(ADMIN_PATH + "/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentRequest))
                .andDo(print())
                .andExpect(status().isCreated());
    }

}