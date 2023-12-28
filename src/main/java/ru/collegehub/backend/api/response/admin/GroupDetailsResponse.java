package ru.collegehub.backend.api.response.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GroupDetailsResponse {
    private Long id;
    private String name;
    private Integer course;
    private String speciality;
}
