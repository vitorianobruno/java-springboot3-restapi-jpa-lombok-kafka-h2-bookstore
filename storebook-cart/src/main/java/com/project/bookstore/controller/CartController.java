package com.project.bookstore.controller;

import com.project.bookstore.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/{customerId}/add")
    public String addBook(@PathVariable Long customerId, @RequestParam String bookTitle) {
        return cartService.addBookToCart(customerId, bookTitle);
    }

    @GetMapping("/{customerId}/checkout")
    public String checkout(@PathVariable Long customerId) {
        double total = cartService.checkout(customerId);
        return "Total amount: $" + total;
    }
}
