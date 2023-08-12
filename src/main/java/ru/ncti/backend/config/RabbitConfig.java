package ru.ncti.backend.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * user: ichuvilin
 */
@Configuration
public class RabbitConfig {

    public static final String EMAIL_UPDATE = "email_update";
    public static final String PUBLIC_CHAT_NOTIFICATION = "public_chat_notification";
    public static final String PRIVATE_CHAT_NOTIFICATION = "private_chat_notification";
    public static final String UPDATE_CLASSROOM = "update_classroom";
    public static final String CHANGE_SCHEDULE = "change_schedule";


    @Bean
    public MessageConverter jsonMessage() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue emailNotificationQueue() {
        return new Queue(EMAIL_UPDATE);
    }

    @Bean
    public Queue publicNotificationQueue() {
        return new Queue(PUBLIC_CHAT_NOTIFICATION);
    }

    @Bean
    public Queue privateNotificationQueue() {
        return new Queue(PRIVATE_CHAT_NOTIFICATION);
    }

    @Bean
    public Queue classroomNotificationQueue() {
        return new Queue(UPDATE_CLASSROOM);
    }

    @Bean
    public Queue scheduleNotificationQueue() {
        return new Queue(CHANGE_SCHEDULE);
    }
}
