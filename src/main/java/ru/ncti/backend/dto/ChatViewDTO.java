package ru.ncti.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */
@Getter
@Setter
@Builder
@ToString
public class ChatViewDTO {
    private UUID id;
    private String name;
    private String type;
}
