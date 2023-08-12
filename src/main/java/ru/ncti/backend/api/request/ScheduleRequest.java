package ru.ncti.backend.api.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class ScheduleRequest {
    private Date date;
    private Long group;
    private Long subject;
    private Integer numberPair;
    private Long teacher;
    private String classroom;
}
