package com.uphill.domain.appointment.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "spring.kafka.topic")
@RequiredArgsConstructor
@Getter
@Setter
public class TopicConfig {
    private String medicReservation;
    private String roomReservation;
    private String emailNotification;
    private int partitionCount;
}
