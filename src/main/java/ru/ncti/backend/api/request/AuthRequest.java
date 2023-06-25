package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class AuthRequest {
    private String username;
    private String password;
}
