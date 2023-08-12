package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
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
