package ru.ncti.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 28-05-2023
 */
@Getter
@Setter
@Builder
public class ChatViewDTO {
    private UUID id;
    private String name;
    private Integer userCount;
}
