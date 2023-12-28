package ru.collegehub.backend.api.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GroupRequest {
    @NotNull
    @Min(value = 1)
    private Integer course;
    @NotBlank
    private String name;
    @NotBlank
    private String speciality;
}
