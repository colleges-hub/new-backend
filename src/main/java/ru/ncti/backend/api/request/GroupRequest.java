package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class GroupRequest {
    private String name;
    private String speciality;
    private Integer course;
}
