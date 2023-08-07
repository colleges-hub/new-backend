package ru.ncti.backend.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class NewsResponse {
    private String title;
    private String info;
    private List<String> images;
}
