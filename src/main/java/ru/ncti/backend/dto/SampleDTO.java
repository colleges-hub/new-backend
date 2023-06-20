package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class SampleDTO {
    private Long group;
    private String day;
    private String parity;
    private Long subject;
    private Integer numberPair;
    private Integer subgroup;
    private Long teacher;
    private String classroom;
}
