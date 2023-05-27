package ru.ncti.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 27-05-2023
 */
@Getter
@Setter
@AllArgsConstructor
public class TeacherScheduleDTO {
    private String firstname;
    private String lastname;
    private String surname;
}