package com.quickbite.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
class PaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
