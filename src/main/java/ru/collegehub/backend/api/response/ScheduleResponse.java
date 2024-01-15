package ru.collegehub.backend.api.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleResponse {
    private Long id;
    private Integer numberPair;
    private String subject;
    private String classroom;
    private List<String> payload;
}
