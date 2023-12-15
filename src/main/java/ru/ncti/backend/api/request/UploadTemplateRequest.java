package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadTemplateRequest {
    private String day;
    private String group;
    private String subject;
    private String teacher;
    private Integer numberPair;
    private Integer subgroup;
    private String weekType;
    private String classroom;
}
