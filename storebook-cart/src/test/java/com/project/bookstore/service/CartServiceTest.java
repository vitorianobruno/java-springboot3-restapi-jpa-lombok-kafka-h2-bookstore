package com.project.bookstore.service;

import com.project.bookstore.model.Book;
import com.project.bookstore.model.Cart;
import com.project.bookstore.model.Customer;
import com.project.bookstore.repository.BookRepository;
import com.project.bookstore.repository.CartRepository;
import com.project.bookstore.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    private Customer customer;
    private Book book;
    private Cart cart;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize test objects
        customer = new Customer();
        customer.setId(1L);
        customer.setName("John Doe");

        book = new Book();
        book.setId(1L);
        book.setTitle("Spring Boot Basics");
        book.setPrice(29.99);

        cart = new Cart();
        customer.setCart(cart);
    }

    @Test
    void testAddBookToCart_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(bookRepository.findByTitle("Spring Boot Basics")).thenReturn(Optional.of(book));

        // Act
        String result = cartService.addBookToCart(1L, "Spring Boot Basics");

        // Assert
        assertEquals("Book 'Spring Boot Basics' added to John Doe's cart", result);
        assertTrue(cart.getBooks().contains(book));
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void testAddBookToCart_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addBookToCart(1L, "Spring Boot Basics"));
        assertEquals("Customer not found.", ex.getMessage());
    }

    @Test
    void testAddBookToCart_BookNotFound() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(bookRepository.findByTitle("Unknown Book")).thenReturn(Optional.empty());

        // Act
        String result = cartService.addBookToCart(1L, "Unknown Book");

        // Assert
        assertEquals("Book not found", result);
        assertTrue(cart.getBooks().isEmpty());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testCheckout_Success() {
        // Arrange
        cart.addBook(book);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act
        double total = cartService.checkout(1L);

        // Assert
        assertEquals(29.99, total, 0.01);
    }

    @Test
    void testCheckout_EmptyCart() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act
        double total = cartService.checkout(1L);

        // Assert
        assertEquals(0.0, total, 0.01);
    }

    @Test
    void testCheckout_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.checkout(1L));
        assertEquals("Customer not found", ex.getMessage());
    }
}

