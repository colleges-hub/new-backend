package ru.collegehub.backend.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassroomRequest {
    private Long id;
    private String classroom;
}
