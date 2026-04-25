package com.project.bookstore.service;

import com.project.bookstore.model.enums.PaymentStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/*
Listener that receives the payment result and completes the checkout
 */
@Service
public class PaymentResultConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CartService cartService;

    public PaymentResultConsumer(KafkaTemplate<String, String> kafkaTemplate, CartService cartService) {
        this.kafkaTemplate = kafkaTemplate;
        this.cartService = cartService;
    }

    @KafkaListener(topics = "payment-result-topic", groupId = "bookstore-service-group")
    public void handlePaymentResult(String message) {
        System.out.println("Bookstore received payment result: " + message);

        // Format: "APPROVED:ORDER_ID:1|CUSTOMER:John|TOTAL:75.98" "DECLINED:ORDER_ID:1|CUSTOMER:John|TOTAL:75.98"
        Long orderId = extractOrderId(message);
        PaymentStatus paymentStatus = extractPaymentStatus(message);

        if (paymentStatus == PaymentStatus.APPROVED) {
            System.out.println("APPROVED Payment for Order: " + orderId);

            cartService.completeCheckout(orderId, PaymentStatus.APPROVED);

            // Publish stock update
            kafkaTemplate.send("stock-update-topic", "UPDATE_STOCK:" + message);
        } else {
            System.out.println("DECLINED Payment for Order: " + orderId);

            cartService.completeCheckout(orderId, PaymentStatus.DECLINED);
        }
    }

    private Long extractOrderId(String message) {
        // Format: "APPROVED:ORDER_ID:1|CUSTOMER:..." o "DECLINED:ORDER_ID:1|..."
        try {
            String[] parts = message.split("\\|");
            String orderPart = parts[0]; // "APPROVED:ORDER_ID:1" o "DECLINED:ORDER_ID:1"
            String orderIdStr = orderPart.split(":")[2]; // Índice 2 porque es "APPROVED:ORDER_ID:1"
            return Long.parseLong(orderIdStr);
        } catch (Exception e) {
            System.err.println("Error extracting ORDER_ID from: " + message);
            throw new RuntimeException("Could not extract ORDER_ID", e);
        }
    }

    private PaymentStatus extractPaymentStatus(String message) {
        if (message.startsWith("APPROVED")) {
            return PaymentStatus.APPROVED;
        } else if (message.startsWith("DECLINED")) {
            return PaymentStatus.DECLINED;
        }
        throw new RuntimeException("Unknown payment status in message: " + message);
    }
}