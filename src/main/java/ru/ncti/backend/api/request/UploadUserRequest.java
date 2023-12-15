package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadUserRequest {
    private String firstname;
    private String lastname;
    private String surname;
    private String email;
    private String group;
    private String role;
    private String password;
}
