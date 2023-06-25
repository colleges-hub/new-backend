package ru.ncti.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
public class ViewChatResponse {
    private UUID id;
    private String name;
    private String type;
    private String photo;
}
