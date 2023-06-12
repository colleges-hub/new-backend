package ru.ncti.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 12-06-2023
 */
@Getter
@Setter
@Builder
public class Email {
    String to;
    String subject;
    String text;
    String template;
    Map<String, Object> properties;
}
