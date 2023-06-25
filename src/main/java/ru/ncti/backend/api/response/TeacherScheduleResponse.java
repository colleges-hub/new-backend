package ru.ncti.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
public class TeacherScheduleResponse {
    private Integer numberPair;
    private String subject;
    private String classroom;
    private List<String> groups;
}
