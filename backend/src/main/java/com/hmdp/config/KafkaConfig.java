package com.hmdp.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    private static final String TOPIC_NAME_VOUCHER_ORDER ="voucher_order";

    @Bean
    public NewTopic topic1(){
        return TopicBuilder.name(TOPIC_NAME_VOUCHER_ORDER)
                .compact()
                .build();
    }
}
