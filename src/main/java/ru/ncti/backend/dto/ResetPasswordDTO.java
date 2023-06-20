package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class ResetPasswordDTO {
    private Long id;
    private String password;
}
