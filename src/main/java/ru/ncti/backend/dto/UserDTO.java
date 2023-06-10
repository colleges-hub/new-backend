package ru.ncti.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 26-05-2023
 */

@Getter
@Setter
@Builder
public class UserDTO {
    private String firstname;
    private String lastname;
    private String surname;
    private String email;
    @JsonIgnore
    private String password;
}
