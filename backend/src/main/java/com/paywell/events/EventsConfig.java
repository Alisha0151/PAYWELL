package com.paywell.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywell.config.PaywellProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventsConfig {

    @Bean
    public DomainEventPublisher domainEventPublisher(PaywellProperties props, ObjectMapper mapper) {
        PaywellProperties.Events.Kafka k = props.events().kafka();
        if (k.enabled()) {
            return new KafkaDomainEventPublisher(k.bootstrapServers(), k.topic(), mapper);
        }
        return new LoggingDomainEventPublisher();
    }
}
