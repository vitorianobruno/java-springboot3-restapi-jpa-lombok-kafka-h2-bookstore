package com.project.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDto {
    private Long orderId;
    private String customerName;
    private Double total;
    private String status;  // PENDING, COMPLETED, FAILED
    private String message;
}