package com.quickbite.payment;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        Dotenv rootDotenv = Dotenv.configure().ignoreIfMissing().load();
        Dotenv serviceDotenv = Dotenv.configure().directory("payment-service").ignoreIfMissing().load();

        String keyId = firstNonBlank(
                System.getProperty("RAZORPAY_KEY_ID"),
                System.getenv("RAZORPAY_KEY_ID"),
                rootDotenv.get("RAZORPAY_KEY_ID"),
                serviceDotenv.get("RAZORPAY_KEY_ID")
        );
        String keySecret = firstNonBlank(
                System.getProperty("RAZORPAY_KEY_SECRET"),
                System.getenv("RAZORPAY_KEY_SECRET"),
                rootDotenv.get("RAZORPAY_KEY_SECRET"),
                serviceDotenv.get("RAZORPAY_KEY_SECRET")
        );

        if (keyId != null && !keyId.isBlank()) {
            System.setProperty("RAZORPAY_KEY_ID", keyId);
        }
        if (keySecret != null && !keySecret.isBlank()) {
            System.setProperty("RAZORPAY_KEY_SECRET", keySecret);
        }
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
