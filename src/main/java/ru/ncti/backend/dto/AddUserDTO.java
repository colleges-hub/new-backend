package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */
@Getter
@Setter
public class AddUserDTO {
    List<Long> ids;
}
