package ru.collegehub.backend.controller;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.collegehub.backend.repository.StudentRepository;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser(value = "admin", authorities = "ADMIN")
class AdminControllerTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

    @Autowired
    private StudentRepository studentRepository;
    private final MockMvc mockMvc;

    AdminControllerTest(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void successfulAddUser_withValidValue() throws Exception {
        mockMvc
                .perform(post("/admin/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstname": "Ivan",
                                    "lastname": "Ivanon",
                                    "email": "ivanov_ivan@list.ru",
                                    "roles" : [
                                        "ADMIN"
                                    ]
                                }
                                  """)
                ).andExpectAll(
                        status().isOk(),
                        jsonPath("$.message").value("User ivanov_ivan@list.ru was created")
                );
    }

    @Test
    void successfulAddStudent_withValidValue() throws Exception {
        mockMvc
                .perform(post("/admin/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstname": "Ivan",
                                    "lastname": "Ivanon",
                                    "email": "ivanov_ivan@list.ru",
                                    "roles" : [
                                        "student"
                                    ],
                                    "groupName": "П-91",
                                    "subgroup": 1
                                }
                                """)
                ).andExpectAll(status().isOk(),
                        jsonPath("$.message").value("User ivanov_ivan@list.ru was created"));
    }

    @Test
    void successfulAddSpeciality_withValidValue() throws Exception {
        mockMvc
                .perform(post("/admin/create-speciality")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "id": "09.02.05",
                                    "name": "Прикладная информатика (по отраслям)"
                                }
                                  """)
                ).andExpectAll(
                        status().isOk(),
                        jsonPath("$.message").value("Speciality 09.02.05 was created")
                );
    }

    @Test
    void successfulAddGroup_withValidValue() throws Exception {
        mockMvc
                .perform(post("/admin/create-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "course": 1,
                                    "name": "Т-31",
                                    "speciality": "09.02.04"
                                }
                                  """)
                ).andExpectAll(
                        status().isOk(),
                        jsonPath("$.message").value("Group Т-31 was created")
                );
    }

    @Test
    void successfulAddSubject_withValidValue() throws Exception {
        mockMvc
                .perform(post("/admin/create-subject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Математика"
                                }
                                  """)
                ).andExpectAll(
                        status().isOk(),
                        jsonPath("$.message").value("Subject Математика was created")
                );
    }

    @Test
    void successfulAddSchedule_withValidValue() throws Exception {
        mockMvc
                .perform(post("/admin/create-schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "dayOfWeek": "15.01.2024",
                                    "group": 1,
                                    "teacher": 1,
                                    "numberPair": 1,
                                    "subject": 1,
                                    "classroom": "Кабинет 200"
                                }
                                """)
                ).andExpectAll(status().isOk(),
                        jsonPath("$.message").value("Schedule was added")
                );
    }

    @Test
    void getUsers() throws Exception {
        mockMvc
                .perform(get("/admin/users")).andExpectAll(
                        status().isOk(),
                        jsonPath("$", hasSize(2))
                );
    }

    @Test
    void getUserById() throws Exception {
        mockMvc
                .perform(get("/admin/users/1"))
                .andExpectAll(status().isOk(),
                        jsonPath("$.firstname").isNotEmpty(),
                        jsonPath("$.lastname").isNotEmpty(),
                        jsonPath("$.email").value("admin@admin.com"),
                        jsonPath("$.roles").isArray()
                );
    }

    @Test
    void getGroups() throws Exception {
        mockMvc
                .perform(get("/admin/groups"))
                .andExpectAll(status().isOk(),
                        jsonPath("$", hasSize(1))
                );
    }

    @Test
    void getGroupById() throws Exception {
        mockMvc
                .perform(get("/admin/groups/1"))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(1),
                        jsonPath("$.name").value("П-91"),
                        jsonPath("$.course").value("4")
                );
    }

    @Test
    void getSpecialities() throws Exception {
        mockMvc.perform(get("/admin/specialities"))
                .andExpectAll(status().isOk(),
                        jsonPath("$", hasSize(1))
                );
    }

    @Test
    void getSpecialityById() throws Exception {
        mockMvc.perform(get("/admin/specialities/09.02.04"))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value("09.02.04"),
                        jsonPath("$.name").isNotEmpty()
                );
    }

    @Test
    void getUserById_withInvalidId() throws Exception {
        mockMvc.perform(
                get("/admin/users/0")
        ).andExpectAll(status().isBadRequest(),
                jsonPath("$.message").isNotEmpty());
    }

    @Test
    void getUserById_withNonExistentId() throws Exception {
        mockMvc.perform(
                get("/admin/users/10")
        ).andExpectAll(status().isBadRequest(),
                jsonPath("$.message").value("User 10 not found"));
    }

    @Test
    void getGroupById_withInvalidId() throws Exception {
        mockMvc.perform(
                get("/admin/groups/0")
        ).andExpectAll(status().isBadRequest(),
                jsonPath("$.message").isNotEmpty());
    }

    @Test
    void getGroupById_withNonExistentId() throws Exception {
        mockMvc.perform(
                get("/admin/groups/10")
        ).andExpectAll(status().isBadRequest(),
                jsonPath("$.message").value("Group 10 not found"));
    }

    @Test
    void failedAddUser_withInvalidData() throws Exception {
        mockMvc.perform(post("/admin/create-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "firstname": "",
                            "lastname": "",
                            "email": "ivanov_ivan"
                        }
                        """)
        ).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.firstname").isNotEmpty(),
                jsonPath("$.lastname").isNotEmpty(),
                jsonPath("$.email").isNotEmpty(),
                jsonPath("$.roles").isNotEmpty()
        );
    }

    @Test
    void failedAddSpeciality_withInvalidData() throws Exception {
        mockMvc
                .perform(post("/admin/create-speciality")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "id": "",
                                    "name": ""
                                }
                                  """)
                ).andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.name").isNotEmpty()
                );
    }

    @Test
    void failedCreateGroup_withInvalidData() throws Exception {
        mockMvc
                .perform(post("/admin/create-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "speciality": ""
                                }
                                  """)
                ).andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.name").isNotEmpty(),
                        jsonPath("$.course").isNotEmpty(),
                        jsonPath("$.speciality").isNotEmpty()
                );
    }

    @Test
    void failedCreateSubject_withInvalidData() throws Exception {
        mockMvc
                .perform(post("/admin/create-subject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": ""
                                }
                                  """)
                ).andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.name").isNotEmpty()
                );
    }

    @Test
    void failedGetSpecialityById_withInvalidId() throws Exception {
        mockMvc.perform(get("/admin/specialities/invalid_spec"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.message").value("Speciality invalid_spec not found")
                );
    }

    @Test
    void failedAddSchedule_withInvalidData() throws Exception {
        mockMvc
                .perform(post("/admin/create-schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "dayOfWeek": "",
                                    "group": null,
                                    "teacher": null,
                                    "numberPair": 1,
                                    "subject": 1,
                                    "classroom": "Кабинет 200"
                                }
                                """)
                ).andExpectAll(status().isBadRequest(),
                        jsonPath("$.dayOfWeek").isNotEmpty(),
                        jsonPath("$.group").isNotEmpty(),
                        jsonPath("$.teacher").isNotEmpty()
                );
    }

}