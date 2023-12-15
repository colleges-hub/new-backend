package ru.collegehub.backend.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupRequest {
    private String name;
    private String speciality;
    private Integer course;
    private String section;
}
