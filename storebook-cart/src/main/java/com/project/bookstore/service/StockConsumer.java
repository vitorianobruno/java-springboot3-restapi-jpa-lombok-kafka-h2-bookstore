package com.project.bookstore.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/*
**THIS IS AN EXAMPLE**
This code should be in the Stock microservice.
 */
@Service
public class StockConsumer {

    @KafkaListener(topics = "stock-update-topic", groupId = "inventory-service-group")
    public void updateStock(String message) {
        System.out.println("📦 Updating stock after approved payment: " + message);
        // Here the code for update the stock database
    }
}

