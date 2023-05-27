package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 26-05-2023
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
