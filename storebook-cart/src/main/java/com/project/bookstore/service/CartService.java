package com.project.bookstore.service;

import com.project.bookstore.dto.CartDetailsDto;
import com.project.bookstore.model.*;
import com.project.bookstore.model.enums.OrderStatus;
import com.project.bookstore.model.enums.PaymentStatus;
import com.project.bookstore.repository.BookRepository;
import com.project.bookstore.repository.CartRepository;
import com.project.bookstore.repository.CustomerRepository;
import com.project.bookstore.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CartService(CustomerRepository customerRepository,
                       BookRepository bookRepository,
                       CartRepository cartRepository,
                       OrderRepository orderRepository,
                       KafkaTemplate<String, String> kafkaTemplate) {
        this.customerRepository = customerRepository;
        this.bookRepository = bookRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public String addBookToCart(Long customerId, String bookTitle) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));

        Optional<Book> bookOpt = bookRepository.findByTitle(bookTitle);
        if (bookOpt.isEmpty()) {
            return "Book not found";
        }

        Book book = bookOpt.get();
        Cart cart = customer.getCart();
        if (cart == null) {
            cart = new Cart();
            cart.setCustomer(customer);
            customer.setCart(cart);
        }
        cart.addBook(book);
        cartRepository.save(cart);

        return "Book '" + book.getTitle() + "' added to " + customer.getName() + "'s cart";
    }

    @Transactional
    public Order checkout(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Cart cart = customer.getCart();
        if (cart == null || cart.getBooks().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double total = cart.getBooks().stream().mapToDouble(Book::getPrice).sum();

        Order order = new Order();
        order.setCustomer(customer);
        order.setTotal(total);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        String event = String.format("ORDER_ID:%d|CUSTOMER:%s|TOTAL:%.2f",
                order.getId(), customer.getName(), total);
        kafkaTemplate.send("checkout-events-topic", event);

        return order;
    }

    // (called from listener)
    @Transactional
    public void completeCheckout(Long orderId, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (paymentStatus == PaymentStatus.APPROVED) {
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());

            Cart cart = order.getCustomer().getCart();

            if (cart != null) {
                System.out.println("BEFORE: Cart tiene " + cart.getBooks().size() + " libros");

                // 1. Clean BD
                cartRepository.clearCartBooks(cart.getId());

                // 2. Clean memory
                cart.getBooks().clear();

                // 3. Save empty cart
                cartRepository.save(cart);

                // 4. Flush
                cartRepository.flush();

                System.out.println("AFTER: Cart tiene " + cart.getBooks().size() + " libros");
            }

            orderRepository.save(order);
        }
        else if (paymentStatus == PaymentStatus.DECLINED) {
            order.setStatus(OrderStatus.FAILED);
            order.setFailedAt(LocalDateTime.now());
            orderRepository.save(order);
        }
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional(readOnly = true)
    public CartDetailsDto getCartDetails(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Cart cart = customer.getCart();
        if (cart == null || cart.getBooks().isEmpty()) {
            return new CartDetailsDto(customerId, customer.getName(), List.of(), 0.0);
        }

        List<CartDetailsDto.BookDto> books = cart.getBooks().stream()
                .map(b -> new CartDetailsDto.BookDto(b.getId(), b.getTitle(), b.getPrice()))
                .toList();

        Double total = books.stream().mapToDouble(CartDetailsDto.BookDto::getPrice).sum();

        return new CartDetailsDto(customerId, customer.getName(), books, total);
    }
}
