package ru.ncti.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 26-05-2023
 */

@Getter
@Setter
public class SampleUploadDTO {
    private String day;
    private String group;
    private String subject;
    private String teacher;
    private Integer numberPair;
    private Integer subgroup;
    private String weekType;
    private String classroom;
}
