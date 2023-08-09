package ru.ncti.backend.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Base64;

/**
 * user: ichuvilin
 */
@Getter
@Setter
public class MessageFileRequest {
    private byte[] file;
}
