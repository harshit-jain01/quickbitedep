package com.quickbite.order.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickbite.order.dto.event.OrderEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

class KafkaConfigTest {

    private final KafkaConfig kafkaConfig = new KafkaConfig();

    @Test
    void producerFactory_shouldCreateFactoryWithConfiguredBootstrapServer() {
        KafkaProperties properties = new KafkaProperties();
        properties.setBootstrapServers(List.of("localhost:9092"));
        ProducerFactory<String, OrderEvent> factory = kafkaConfig.producerFactory(properties);

        assertNotNull(factory);
        Object bootstrap = factory.getConfigurationProperties().get("bootstrap.servers");
        assertTrue(bootstrap.toString().contains("localhost:9092"));
    }

    @Test
    void kafkaTemplate_shouldCreateTemplateFromFactory() {
        KafkaProperties properties = new KafkaProperties();
        properties.setBootstrapServers(List.of("localhost:9092"));
        ProducerFactory<String, OrderEvent> factory = kafkaConfig.producerFactory(properties);

        KafkaTemplate<String, OrderEvent> template = kafkaConfig.kafkaTemplate(factory);

        assertNotNull(template);
    }
}
