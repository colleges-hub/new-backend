package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class TemplateRequest {
    private String day;
    private Long group;
    private Long subject;
    private String parity;
    private Integer numberPair;
    private Integer subgroup;
    private Long teacher;
    private String classroom;
}
