package ru.ncti.backend.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ncti.backend.model.Role;

import java.util.Set;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String firstname;
    private String lastname;
    private String surname;
    private String photo;
    private String email;
    private Set<Role> role;
    private String chat;
}
