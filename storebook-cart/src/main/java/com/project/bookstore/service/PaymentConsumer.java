package com.project.bookstore.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/*
**THIS IS AN EXAMPLE**
This code should be in the Payments microservice.
 */
@Service
public class PaymentConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentConsumer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "checkout-events-topic", groupId = "payment-service-group")
    public void processPayment(String message) {
        System.out.println("💳 Processing payment: " + message);

        // Payment logic simulation
        boolean approved = Math.random() > 0.2; // 80% éxito
        if (approved) {
            kafkaTemplate.send("payment-result-topic", "APPROVED:" + message);
        } else {
            kafkaTemplate.send("payment-result-topic", "DECLINED:" + message);
        }
    }
}


