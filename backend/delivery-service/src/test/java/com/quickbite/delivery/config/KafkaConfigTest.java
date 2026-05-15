package com.quickbite.delivery.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.quickbite.delivery.dto.event.OrderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

class KafkaConfigTest {

    private final KafkaConfig kafkaConfig = new KafkaConfig();

    @Test
    void consumerFactory_shouldBuildFactory() {
        ConsumerFactory<String, OrderEvent> factory = kafkaConfig.consumerFactory("localhost:9092", "delivery-group");
        assertNotNull(factory);
    }

    @Test
    void kafkaListenerContainerFactory_shouldSetConsumerFactory() {
        ConsumerFactory<String, OrderEvent> consumerFactory = kafkaConfig.consumerFactory("localhost:9092", "delivery-group");

        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
                kafkaConfig.kafkaListenerContainerFactory(consumerFactory);

        assertNotNull(factory.getConsumerFactory());
    }
}
