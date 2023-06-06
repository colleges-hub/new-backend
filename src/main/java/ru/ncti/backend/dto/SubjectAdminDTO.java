package ru.ncti.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 06-06-2023
 */
@Getter
@Setter
@Builder
public class SubjectAdminDTO {
    private Long id;
    private String name;
}
