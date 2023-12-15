package ru.ncti.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class SectionResponse {
    private String name;
    private String email;
}
