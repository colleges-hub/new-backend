package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@ToString
public class ScheduleChangeRequest {
    private List<String> group;
    private String subject;
    private String date;
    private Integer numberPair;
    private String classroom;
}
