package ru.ncti.backend.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import ru.ncti.backend.service.RedisService;

import java.util.Objects;

/**
 * user: ichuvilin
 */
@Log4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RedisService redisService;

    @EventListener
    public void handlerSubscribe(SessionSubscribeEvent event) {
        String uuid = Objects.requireNonNull(event.getMessage()
                .getHeaders().get("simpDestination")).toString().split("/")[3].trim();
        String name = Objects.requireNonNull(event.getUser()).getName();
        redisService.setValue("user:" + name, uuid);
        redisService.setValueSet(uuid, name);
        log.info(event);
    }

    @EventListener
    public void handlerUnsubscribe(SessionUnsubscribeEvent event) {
        String name = Objects.requireNonNull(event.getUser()).getName();
        String key = String.format("user:%s", name);
        String chat = redisService.getValue(key);
        redisService.deleteValue(key);
        redisService.deleteValueSet(chat, name);
        log.info(event);
    }

    // todo: handler for event to disconnect

}
