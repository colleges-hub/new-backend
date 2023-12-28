package ru.collegehub.backend.api.request.admin;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRequest {
    @NotBlank
    private String firstname;
    @NotBlank
    private String lastname;
    private String patronymic;
    @Email
    @NotBlank
    private String email;
    @NotEmpty
    private List<String> roles;
    private String groupName;
    private Integer subgroup;
}
