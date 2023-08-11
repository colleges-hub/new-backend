package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class FCMRequest {
    String token;
    String device;
}
