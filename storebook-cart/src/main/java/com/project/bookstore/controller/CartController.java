package com.project.bookstore.controller;

import com.project.bookstore.dto.CheckoutResponseDto;
import com.project.bookstore.dto.ErrorResponseDto;
import com.project.bookstore.model.Order;
import com.project.bookstore.model.enums.OrderStatus;
import com.project.bookstore.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/{customerId}/add")
    public ResponseEntity<String> addBook(@PathVariable Long customerId,
                                          @RequestParam String bookTitle) {
        try {
            String result = cartService.addBookToCart(customerId, bookTitle);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{customerId}/checkout")
    public ResponseEntity<?> checkout(@PathVariable Long customerId) {
        try {
            Order order = cartService.checkout(customerId);

            CheckoutResponseDto response = new CheckoutResponseDto(
                    order.getId(),
                    order.getCustomer().getName(),
                    order.getTotal(),
                    order.getStatus().toString(),
                    "Order created successfully. Waiting for payment confirmation."
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponseDto("Checkout failed", e.getMessage())
            );
        }
    }

    // Use to get the order status
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrderStatus(@PathVariable Long orderId) {
        try {
            Order order = cartService.getOrder(orderId);

            CheckoutResponseDto response = new CheckoutResponseDto(
                    order.getId(),
                    order.getCustomer().getName(),
                    order.getTotal(),
                    order.getStatus().toString(),
                    getStatusMessage(order.getStatus())
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Use to get customer cart
    @GetMapping("/{customerId}")
    public ResponseEntity<?> getCart(@PathVariable Long customerId) {
        try {
            var cartDto = cartService.getCartDetails(customerId);
            return ResponseEntity.ok(cartDto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponseDto("Error retrieving cart", e.getMessage())
            );
        }
    }

    private String getStatusMessage(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Order created. Processing payment...";
            case COMPLETED -> "Payment approved! Order completed.";
            case FAILED -> "Payment declined. Please try again.";
        };
    }
}