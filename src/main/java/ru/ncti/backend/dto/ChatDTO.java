package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class ChatDTO {
    private String name;
    private List<Long> ids;
}
