package ru.ncti.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
public class UserMessageResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String photo;
}
