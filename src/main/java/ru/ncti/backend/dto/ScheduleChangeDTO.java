package ru.ncti.backend.dto;

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
public class ScheduleChangeDTO {
    private List<String> group;
    private String subject;
    private String date;
    private Integer numberPair;
    private String classroom;
}
