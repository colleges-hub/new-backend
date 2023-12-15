package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TemplateRequest {
    private String day;
    private Long group;
    private Long subject;
    private String parity;
    private Integer numberPair;
    private Long teacher;
    private String classroom;
}
