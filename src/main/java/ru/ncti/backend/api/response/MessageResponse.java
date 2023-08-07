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
public class MessageResponse {
    private UUID id;
    private String text;
    private UserMessageResponse author;
    private Long createdAt;
    private String uri;
    private String name;
    private Integer size;
    private String type;
}
