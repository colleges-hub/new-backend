package ru.collegehub.backend.api.response.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class UserResponse {
    private Long id;
    private String firstname;
    private String lastname;
    private String patronymic;
    private List<String> roles;
}
