package ru.collegehub.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class UserProfileResponse {
    private String firstname;
    private String lastname;
    private String patronymic;
    private List<String> roles;
}
