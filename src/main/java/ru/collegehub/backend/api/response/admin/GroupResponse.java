package ru.collegehub.backend.api.response.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GroupResponse {
    private Long id;
    private String name;
}
