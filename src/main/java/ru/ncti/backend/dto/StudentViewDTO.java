package ru.ncti.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ncti.backend.entity.Role;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 27-05-2023
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentViewDTO {
    private String firstname;
    private String lastname;
    private String surname;
    private String email;
    private String group;
    private String speciality;
    private Integer course;
    private Set<Role> role;
}
