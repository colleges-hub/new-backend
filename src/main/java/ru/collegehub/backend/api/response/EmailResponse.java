package ru.collegehub.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


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
