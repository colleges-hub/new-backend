package ru.collegehub.backend.api.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RoleResponse {
    private Long id;
    private String name;
}
