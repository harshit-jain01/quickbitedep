package com.quickbite.payment.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickbite.payment.dto.event.OrderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

class KafkaConfigTest {

    private final KafkaConfig kafkaConfig = new KafkaConfig();

    @Test
    void consumerFactory_shouldContainBootstrapAndGroupProperties() {
        ConsumerFactory<String, OrderEvent> factory = kafkaConfig.consumerFactory("localhost:9092", "group1");

        assertNotNull(factory);
        assertTrue(factory.getConfigurationProperties().get("bootstrap.servers").toString().contains("localhost:9092"));
        assertEquals("group1", factory.getConfigurationProperties().get("group.id"));
    }

    @Test
    void kafkaListenerContainerFactory_shouldUseProvidedConsumerFactory() {
        ConsumerFactory<String, OrderEvent> factory = kafkaConfig.consumerFactory("localhost:9092", "group1");

        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> containerFactory =
                kafkaConfig.kafkaListenerContainerFactory(factory);

        assertNotNull(containerFactory);
        assertNotNull(containerFactory.getConsumerFactory());
    }
}
