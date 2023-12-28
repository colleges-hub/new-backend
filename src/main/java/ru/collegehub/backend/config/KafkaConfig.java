package ru.collegehub.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopics newTopics() {
        return new NewTopics(
                new NewTopic("student", 1, (short) 1)
        );
    }

}
