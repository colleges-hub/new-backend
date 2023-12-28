package ru.collegehub.backend.api.response.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class UserDetailsResponse {
    private Long id;
    private String firstname;
    private String lastname;
    private String patronymic;
    private String email;
    private List<String> roles;
}
