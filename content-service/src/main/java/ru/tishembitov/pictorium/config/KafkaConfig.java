package ru.tishembitov.pictorium.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.content-events:content-events}")
    private String contentEventsTopic;

    @Bean
    public NewTopic contentEventsTopic() {
        return TopicBuilder.name(contentEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}