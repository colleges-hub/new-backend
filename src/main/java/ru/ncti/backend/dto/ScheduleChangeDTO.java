package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 30-05-2023
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
