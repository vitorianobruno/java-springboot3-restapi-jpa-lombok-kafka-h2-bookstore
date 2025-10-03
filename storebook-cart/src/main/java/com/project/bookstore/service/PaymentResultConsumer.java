package com.project.bookstore.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/*
Listener that receive the payment result
 */
@Service
public class PaymentResultConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentResultConsumer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "payment-result-topic", groupId = "bookstore-service-group")
    public void handlePaymentResult(String message) {
        System.out.println("Bookstore received payment result: " + message);

        if (message.startsWith("APPROVED")) {
            // Publish event to update stock > to Stock microservice
            kafkaTemplate.send("stock-update-topic", "UPDATE_STOCK:" + message);
        } else {
            System.out.println("Payment failed, no stock update");
        }
    }
}

