package ru.ncti.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Id;
import java.util.HashSet;
import java.util.Set;

/**
 * user: ichuvilin
 */
@Getter
@Setter
@Builder
@RedisHash("user_in_chat")
public class UserInChat {
    @Id
    private String id;
    private Set<String> email = new HashSet<>();
}
