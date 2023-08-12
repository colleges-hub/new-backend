package ru.ncti.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisServiceTest {

    @Mock
    private ValueOperations<String, String> valueOperationsMock;

    @Mock
    private SetOperations<String, String> setOperationsMock;

    @Mock
    private RedisTemplate<String, String> redisTemplateMock;

    private RedisService redisService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);
        when(redisTemplateMock.opsForSet()).thenReturn(setOperationsMock);
        redisService = new RedisService(redisTemplateMock);
    }

    @Test
    public void testSetValue() {
        String key = "testKey";
        String value = "testValue";
        redisService.setValue(key, value);
        verify(valueOperationsMock).set(key, value);
    }

    @Test
    public void testGetValue() {
        String key = "testKey";
        String expectedValue = "testValue";
        when(valueOperationsMock.get(key)).thenReturn(expectedValue);
        String actualValue = redisService.getValue(key);
        verify(valueOperationsMock).get(key);
        assertEquals(expectedValue, actualValue);
    }

    @Test
    public void testDeleteValue() {
        String key = "testKey";
        redisService.deleteValue(key);
        verify(redisTemplateMock).delete(key);
    }

    @Test
    public void testSetValueSet() {
        String key = "testSetKey";
        String value = "testSetValue";
        redisService.setValueSet(key, value);
        verify(setOperationsMock).add(key, value);
    }

    @Test
    public void testDeleteValueSet() {
        String key = "testSetKey";
        String value = "testSetValue";
        redisService.deleteValueSet(key, value);
        verify(setOperationsMock).remove(key, value);
    }

    @Test
    public void testGetValueSet() {
        String key = "testSetKey";
        Set<String> expectedSet = new HashSet<>();
        expectedSet.add("value1");
        expectedSet.add("value2");
        when(setOperationsMock.members(key)).thenReturn(expectedSet);
        Set<String> actualSet = redisService.getValueSet(key);
        verify(setOperationsMock).members(key);
        assertEquals(expectedSet, actualSet);
    }

}