package ru.collegehub.backend.api.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleRequest {
    @NotBlank
    private String dayOfWeek;
    @NotNull
    private Long group;
    @NotNull
    private Long teacher;
    @NotNull
    private Integer numberPair;
    @NotNull
    private Long subject;
    @NotNull
    private String classroom;
}
