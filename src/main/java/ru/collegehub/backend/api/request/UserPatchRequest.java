package ru.collegehub.backend.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPatchRequest {
    private String firstname;
    private String lastname;
    private String patronymic;
    private String email;
    private String password;
}
