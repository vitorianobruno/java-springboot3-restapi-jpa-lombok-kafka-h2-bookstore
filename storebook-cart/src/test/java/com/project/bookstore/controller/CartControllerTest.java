package com.project.bookstore.controller;

import com.project.bookstore.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class) // load only the controller, not the full Spring context
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc; // mock HTTP client to test controllers

    @MockBean
    private CartService cartService; // mock service dependency

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
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(content().string("Book added successfully")); // response body
    }

    @Test
    @DisplayName("Should checkout cart and return total amount")
    void testCheckoutCart() throws Exception {
        // Arrange (given)
        Long customerId = 1L;
        Mockito.when(cartService.checkout(eq(customerId))).thenReturn(49.99);

        // Act & Assert
        mockMvc.perform(get("/api/cart/{customerId}/checkout", customerId))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(content().string("Total amount: $49.99")); // response body
    }
}

