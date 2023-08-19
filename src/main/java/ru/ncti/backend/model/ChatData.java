package ru.ncti.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Id;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
@RedisHash("chat_data")
public class ChatData {
    @Id
    private String id;
    private String chat;
    private String username;
}
