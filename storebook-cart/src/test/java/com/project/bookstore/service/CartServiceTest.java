package com.project.bookstore.service;

import com.project.bookstore.model.*;
import com.project.bookstore.model.enums.OrderStatus;
import com.project.bookstore.model.enums.PaymentStatus;
import com.project.bookstore.repository.BookRepository;
import com.project.bookstore.repository.CartRepository;
import com.project.bookstore.repository.CustomerRepository;
import com.project.bookstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CartServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;  // ✅ NUEVO

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;  // ✅ NUEVO

    @InjectMocks
    private CartService cartService;

    private Customer customer;
    private Book book;
    private Cart cart;
    private Order order;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize Customer
        customer = new Customer();
        customer.setId(1L);
        customer.setName("John Doe");

        // Initialize Book
        book = new Book();
        book.setId(1L);
        book.setTitle("Spring Boot Basics");
        book.setPrice(29.99);

        // Initialize Cart
        cart = new Cart();
        cart.setId(1L);
        cart.setCustomer(customer);
        customer.setCart(cart);

        // Initialize Order
        order = new Order();
        order.setId(100L);
        order.setCustomer(customer);
        order.setTotal(29.99);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should add book to cart successfully")
    void testAddBookToCart_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(bookRepository.findByTitle("Spring Boot Basics")).thenReturn(Optional.of(book));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        String result = cartService.addBookToCart(1L, "Spring Boot Basics");

        // Assert
        assertEquals("Book 'Spring Boot Basics' added to John Doe's cart", result);
        assertTrue(cart.getBooks().contains(book));
        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString());  // No Kafka en add
    }

    @Test
    @DisplayName("Should throw exception when customer not found in addBookToCart")
    void testAddBookToCart_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addBookToCart(1L, "Spring Boot Basics"));
        assertEquals("Customer not found.", ex.getMessage());
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return message when book not found")
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
    @DisplayName("Should create new cart if it doesn't exist")
    void testAddBookToCart_CreateNewCart() {
        // Arrange
        customer.setCart(null);  // Customer sin carrito
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(bookRepository.findByTitle("Spring Boot Basics")).thenReturn(Optional.of(book));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart savedCart = invocation.getArgument(0);
            savedCart.setId(1L);
            return savedCart;
        });

        // Act
        String result = cartService.addBookToCart(1L, "Spring Boot Basics");

        // Assert
        assertEquals("Book 'Spring Boot Basics' added to John Doe's cart", result);
        assertNotNull(customer.getCart());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should checkout and create Order with PENDING status")
    void testCheckout_Success() {
        // Arrange
        cart.addBook(book);
        order.setTotal(29.99);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = cartService.checkout(1L);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(29.99, result.getTotal(), 0.01);

        // Verificar que se publicó evento a Kafka
        verify(kafkaTemplate, times(1)).send(eq("checkout-events-topic"), anyString());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when cart is empty")
    void testCheckout_EmptyCart() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.checkout(1L));
        assertEquals("Cart is empty", ex.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when customer not found in checkout")
    void testCheckout_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.checkout(1L));
        assertEquals("Customer not found", ex.getMessage());
    }

    @Test
    @DisplayName("Should complete checkout with APPROVED status and clear cart")
    void testCompleteCheckout_Approved() {
        // Arrange
        assertTrue(cart.getBooks().isEmpty());
        cart.addBook(book);
        assertTrue(cart.getBooks().size() == 1);

        order.setStatus(OrderStatus.PENDING);
        order.setCustomer(customer);
        customer.setCart(cart);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        // Act
        cartService.completeCheckout(100L, PaymentStatus.APPROVED);

        // Assert
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertNotNull(order.getCompletedAt());
        assertTrue(cart.getBooks().isEmpty());
        verify(orderRepository, times(1)).save(order);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should complete checkout with DECLINED status without clearing cart")
    void testCompleteCheckout_Declined() {
        // Arrange
        cart.addBook(book);
        int originalSize = cart.getBooks().size();

        order.setStatus(OrderStatus.PENDING);
        order.setCustomer(customer);
        customer.setCart(cart);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        // Act
        cartService.completeCheckout(100L, PaymentStatus.DECLINED);

        // Assert
        assertEquals(OrderStatus.FAILED, order.getStatus());
        assertNotNull(order.getFailedAt());
        assertEquals(originalSize, cart.getBooks().size());
        assertTrue(cart.getBooks().contains(book));
        verify(orderRepository, times(1)).save(order);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when order not found in completeCheckout")
    void testCompleteCheckout_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.completeCheckout(999L, PaymentStatus.APPROVED));
        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    @DisplayName("Should retrieve order by ID")
    void testGetOrder_Success() {
        // Arrange
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        // Act
        Order result = cartService.getOrder(100L);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(29.99, result.getTotal(), 0.01);
    }

    @Test
    @DisplayName("Should throw exception when order not found in getOrder")
    void testGetOrder_NotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.getOrder(999L));
        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    @DisplayName("Should return cart details with books and total")
    void testGetCartDetails_Success() {
        // Arrange
        cart.addBook(book);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act
        var cartDetails = cartService.getCartDetails(1L);

        // Assert
        assertNotNull(cartDetails);
        assertEquals(1L, cartDetails.getCustomerId());
        assertEquals("John Doe", cartDetails.getCustomerName());
        assertEquals(1, cartDetails.getBooks().size());
        assertEquals(29.99, cartDetails.getTotal(), 0.01);
    }

    @Test
    @DisplayName("Should return empty cart details when cart is null")
    void testGetCartDetails_EmptyCart() {
        // Arrange
        customer.setCart(null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act
        var cartDetails = cartService.getCartDetails(1L);

        // Assert
        assertNotNull(cartDetails);
        assertEquals(1L, cartDetails.getCustomerId());
        assertEquals("John Doe", cartDetails.getCustomerName());
        assertTrue(cartDetails.getBooks().isEmpty());
        assertEquals(0.0, cartDetails.getTotal(), 0.01);
    }

    @Test
    @DisplayName("Should throw exception when customer not found in getCartDetails")
    void testGetCartDetails_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.getCartDetails(999L));
        assertEquals("Customer not found", ex.getMessage());
    }
}