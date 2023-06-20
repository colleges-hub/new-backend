package ru.ncti.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
public class GroupViewDTO {
    private Long id;
    private String name;
    private Integer course;
    private String speciality;
}
