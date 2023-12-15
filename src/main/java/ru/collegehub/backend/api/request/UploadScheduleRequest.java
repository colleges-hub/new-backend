package ru.collegehub.backend.api.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UploadScheduleRequest {
    private String date;
    private String group;
    private String subject;
    private Integer numberPair;
    private String teacher;
    private String classroom;
}
