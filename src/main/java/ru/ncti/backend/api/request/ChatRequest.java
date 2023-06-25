package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class ChatRequest {
    private String name;
    private List<Long> ids;
}
