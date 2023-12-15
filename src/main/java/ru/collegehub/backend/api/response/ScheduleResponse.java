package ru.collegehub.backend.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleResponse {
    private String day;
    private Integer numberPair;
    private String subject;
    private List<String> data;
    private String classroom;
}
