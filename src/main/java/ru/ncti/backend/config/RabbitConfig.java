package ru.ncti.backend.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ncti.backend.dto.RabbitQueue;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 03-06-2023
 */
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter jsonMessage() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(RabbitQueue.EMAIL_UPDATE);
    }

    @Bean
    public Queue publicNotificationQueue() {
        return new Queue(RabbitQueue.PUBLIC_CHAT_NOTIFICATION);
    }

    @Bean
    public Queue privateNotificationQueue() {
        return new Queue(RabbitQueue.PRIVATE_CHAT_NOTIFICATION);
    }

    @Bean
    public Queue scheduleNotificationQueue() {
        return new Queue(RabbitQueue.UPDATE_SCHEDULE);
    }
}
