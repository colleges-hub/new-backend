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
public class GroupDTO {
    private String name;
    private String speciality;
    private Integer course;
}
