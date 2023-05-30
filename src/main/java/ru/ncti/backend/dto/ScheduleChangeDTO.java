package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 30-05-2023
 */
@Getter
@Setter
public class ScheduleChangeDTO {
    private List<String> group;
    private String subject;
    private Integer numberPair;
    private String classroom;
}
