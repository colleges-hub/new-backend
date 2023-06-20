package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class StudentDTO {
    private String firstname;
    private String lastname;
    private String surname;
    private String email;
    private String password;
    private String group;
}
