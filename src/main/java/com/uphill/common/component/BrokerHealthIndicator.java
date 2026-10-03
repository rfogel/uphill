package com.uphill.common.component;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BrokerHealthIndicator implements HealthIndicator {

    private final KafkaListenerEndpointRegistry registry;

    @Override
    public Health health() {
        boolean allRunning = registry.getListenerContainers().stream().allMatch(MessageListenerContainer::isRunning);

        if (allRunning) {
            return Health.up().withDetail("consumers", "All listeners active").build();
        }
        return Health.down().withDetail("consumers", "One or more listeners stopped").build();
    }
}
