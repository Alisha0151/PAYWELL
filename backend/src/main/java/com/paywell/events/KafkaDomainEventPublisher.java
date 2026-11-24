package com.paywell.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Plain kafka-clients producer (no Spring Kafka auto-configuration). Failures never break a payment. */
public class KafkaDomainEventPublisher implements DomainEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);
    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final ObjectMapper mapper;

    public KafkaDomainEventPublisher(String bootstrapServers, String topic, ObjectMapper mapper) {
        Properties p = new Properties();
        p.put("bootstrap.servers", bootstrapServers);
        p.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.put("acks", "all");
        p.put("enable.idempotence", "true");
        p.put("max.block.ms", "2000");
        p.put("delivery.timeout.ms", "10000");
        this.producer = new KafkaProducer<>(p);
        this.topic = topic;
        this.mapper = mapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String json = mapper.writeValueAsString(event);
            producer.send(new ProducerRecord<>(topic, event.aggregateId(), json), (meta, ex) -> {
                if (ex != null) log.warn("Kafka publish failed type={} reason={}", event.type(), ex.getClass().getSimpleName());
            });
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Kafka publish skipped type={} reason={}", event.type(), e.getClass().getSimpleName());
        }
    }

    @PreDestroy
    void close() {
        producer.close(Duration.ofSeconds(2));
    }
}
