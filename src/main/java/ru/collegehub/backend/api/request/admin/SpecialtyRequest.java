package ru.collegehub.backend.api.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpecialtyRequest {
    @NotBlank
    private String id;
    @NotBlank
    private String name;
}
