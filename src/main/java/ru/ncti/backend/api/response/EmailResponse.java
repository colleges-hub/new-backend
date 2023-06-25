package ru.ncti.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
public class EmailResponse {
    String to;
    String subject;
    String text;
    String template;
    Map<String, Object> properties;
}
