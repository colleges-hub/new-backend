package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */
@Getter
@Setter
@ToString
public class MessageDTO {
    private Long senderId;
    private String text;
}
