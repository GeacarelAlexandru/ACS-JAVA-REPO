package com.instakill.infrastructure.config;

import com.instakill.interaction.domain.DomainEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class NoOpDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publish(Object event) {
        // Intentionally empty: events are discarded until a real publisher is plugged in.
    }
}
