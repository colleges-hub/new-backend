package ru.collegehub.backend.api.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ScheduleRequest {
    private LocalDate date;
    private Long group;
    private Long subject;
    private Integer numberPair;
    private Long teacher;
    private String classroom;
}
