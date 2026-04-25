package com.project.bookstore.controller;

import com.project.bookstore.model.*;
import com.project.bookstore.model.enums.OrderStatus;
import com.project.bookstore.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    private Customer customer;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setName("John Doe");

        order = new Order();
        order.setId(100L);
        order.setCustomer(customer);  // ← CRÍTICO
        order.setTotal(49.99);
        order.setStatus(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Should add book to customer's cart")
    void testAddBookToCart() throws Exception {
        // Arrange (given)
        Long customerId = 1L;
        String bookTitle = "Spring Boot Basics";
        Mockito.when(cartService.addBookToCart(eq(customerId), eq(bookTitle)))
                .thenReturn("Book added successfully");

        // Act & Assert
        mockMvc.perform(post("/api/cart/{customerId}/add?bookTitle={title}", customerId, bookTitle))
                .andExpect(status().isOk())
                .andExpect(content().string("Book added successfully"));
    }

    @Test
    @DisplayName("Should checkout cart and return Order with PENDING status")
    void testCheckoutCart_Success() throws Exception {
        // Arrange (given)
        Long customerId = 1L;
        Mockito.when(cartService.checkout(eq(customerId))).thenReturn(order);

        // Act & Assert
        mockMvc.perform(post("/api/cart/{customerId}/checkout", customerId))
                .andExpect(status().isCreated())  // HTTP 201
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.total").value(49.99))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return 400 when checkout fails (empty cart)")
    void testCheckoutCart_EmptyCart() throws Exception {
        // Arrange (given)
        Long customerId = 1L;
        Mockito.when(cartService.checkout(eq(customerId)))
                .thenThrow(new RuntimeException("Cart is empty"));

        // Act & Assert
        mockMvc.perform(post("/api/cart/{customerId}/checkout", customerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Checkout failed"));
    }

    @Test
    @DisplayName("Should return 400 when customer not found")
    void testCheckoutCart_CustomerNotFound() throws Exception {
        // Arrange (given)
        Long customerId = 999L;
        Mockito.when(cartService.checkout(eq(customerId)))
                .thenThrow(new RuntimeException("Customer not found"));

        // Act & Assert
        mockMvc.perform(post("/api/cart/{customerId}/checkout", customerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Checkout failed"));
    }

    @Test
    @DisplayName("Should get order status by order ID")
    void testGetOrderStatus_Success() throws Exception {
        // Arrange (given)
        Long orderId = 100L;
        Mockito.when(cartService.getOrder(eq(orderId))).thenReturn(order);

        // Act & Assert
        mockMvc.perform(get("/api/cart/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should return 404 when order not found")
    void testGetOrderStatus_NotFound() throws Exception {
        // Arrange (given)
        Long orderId = 999L;
        Mockito.when(cartService.getOrder(eq(orderId)))
                .thenThrow(new RuntimeException("Order not found"));

        // Act & Assert
        mockMvc.perform(get("/api/cart/order/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }
}