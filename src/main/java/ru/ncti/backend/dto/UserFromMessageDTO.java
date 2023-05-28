package ru.ncti.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFromMessageDTO {
    private String id;
    private String firstName;
    private String lastName;
}
