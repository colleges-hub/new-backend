package ru.ncti.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * user: ichuvilin
 */
@Component
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }

    public void setValueSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    public void deleteValueSet(String key, String value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    public Set<String> getValueSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }
}
