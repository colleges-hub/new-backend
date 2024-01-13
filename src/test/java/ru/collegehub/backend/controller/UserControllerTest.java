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
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import ru.collegehub.backend.api.request.AuthRequest;
import ru.collegehub.backend.api.request.UserPatchRequest;
import ru.collegehub.backend.api.response.AuthResponse;
import ru.collegehub.backend.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class UserControllerTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getProfile_ShouldReturnOk() throws Exception {
        var request = new AuthRequest();

        request.setEmail("petr@yandex.ru");
        request.setPassword("admin");

        var response = restTemplate.postForEntity("http://localhost:" + port + "/auth/signin", request, AuthResponse.class);

        if (response.getBody() == null) {
            throw new Exception("Response body is null");
        }

        mockMvc.perform(get("/user").header("Authorization", "Bearer " + response.getBody().getToken()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.firstname").value("Petr"),
                        jsonPath("$.lastname").value("Petrov")
                );
    }

    @Test
    void updateUser_ShouldReturnOk() throws Exception {
        var request = new AuthRequest();

        request.setEmail("petr@yandex.ru");
        request.setPassword("admin");

        var response = restTemplate.postForEntity("http://localhost:" + port + "/auth/signin", request, AuthResponse.class);

        if (response.getBody() == null) {
            throw new Exception("Response body is null");
        }

        // Создаем объект UserPatchRequest для обновления данных
        var patchRequest = new UserPatchRequest();
        patchRequest.setFirstname("NewFirstName");
        patchRequest.setLastname("NewLastName");

        // Выполняем запрос на обновление данных пользователя
        mockMvc.perform(patch("/user/update")
                        .header("Authorization", "Bearer " + response.getBody().getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(patchRequest))
                )
                .andExpect(status().isOk());

        // Проверяем, что данные пользователя были обновлены в репозитории
        var updatedUser = userRepository.findByEmail("petr@yandex.ru").orElse(null);
        assertNotNull(updatedUser);
        assertEquals("NewFirstName", updatedUser.getFirstname());
        assertEquals("NewLastName", updatedUser.getLastname());
    }

    @Test
    void getScheduleByStudent_ShouldReturnOk() throws Exception {
        var request = new AuthRequest();

        request.setEmail("petr@yandex.ru");
        request.setPassword("admin");

        var response = restTemplate.postForEntity("http://localhost:" + port + "/auth/signin", request, AuthResponse.class);

        if (response.getBody() == null) {
            throw new Exception("Response body is null");
        }
        mockMvc.perform(get("/user/schedule").header("Authorization", "Bearer " + response.getBody().getToken()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$." + LocalDate.now()).isArray()
                );
    }

    @Test
    void getScheduleByTeacher_ShouldReturnOk() throws Exception {
        var request = new AuthRequest();

        request.setEmail("vasilievna@gmail.com");
        request.setPassword("admin");

        var response = restTemplate.postForEntity("http://localhost:" + port + "/auth/signin", request, AuthResponse.class);

        if (response.getBody() == null) {
            throw new Exception("Response body is null");
        }
        mockMvc.perform(get("/user/schedule").header("Authorization", "Bearer " + response.getBody().getToken()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$." + LocalDate.now()).isArray()
                );
    }

    @Test
    void getScheduleByGroupId_ShouldReturnOk() throws Exception {
        var request = new AuthRequest();

        request.setEmail("vasilievna@gmail.com");
        request.setPassword("admin");

        var response = restTemplate.postForEntity("http://localhost:" + port + "/auth/signin", request, AuthResponse.class);

        if (response.getBody() == null) {
            throw new Exception("Response body is null");
        }
        mockMvc.perform(get("/user/schedule?id=1").header("Authorization", "Bearer " + response.getBody().getToken()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$." + LocalDate.now()).isArray()
                );
    }

    @Test
    void getScheduleByGroupId_ShouldReturnBadRequest() throws Exception {
        var request = new AuthRequest();

        request.setEmail("vasilievna@gmail.com");
        request.setPassword("admin");

        var response = restTemplate.postForEntity("http://localhost:" + port + "/auth/signin", request, AuthResponse.class);

        if (response.getBody() == null) {
            throw new Exception("Response body is null");
        }
        mockMvc.perform(get("/user/schedule?id=0").header("Authorization", "Bearer " + response.getBody().getToken()))
                .andExpect(status().isBadRequest());
    }

    private String asJsonString(Object object) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(object);
    }

}
