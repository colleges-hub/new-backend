package ru.ncti.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
public class SectionResponse {
    private String name;
    private String email;
}
