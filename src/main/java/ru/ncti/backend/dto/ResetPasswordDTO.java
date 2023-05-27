package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 27-05-2023
 */
@Getter
@Setter
public class ResetPasswordDTO {
    private Long id;
    private String password;
}
