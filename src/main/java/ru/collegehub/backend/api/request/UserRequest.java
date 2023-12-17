package ru.collegehub.backend.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRequest {
    private String firstname;
    private String lastname;
    private String surname;
    private String email;
    private String group;
    private List<String> role;
}