package com.project.bookstore.service;

import com.project.bookstore.model.Book;
import com.project.bookstore.model.Cart;
import com.project.bookstore.model.Customer;
import com.project.bookstore.repository.BookRepository;
import com.project.bookstore.repository.CartRepository;
import com.project.bookstore.repository.CustomerRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;
    private final CartRepository cartRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CartService(CustomerRepository customerRepository,
                       BookRepository bookRepository,
                       CartRepository cartRepository,
                       KafkaTemplate<String, String> kafkaTemplate) {
        this.customerRepository = customerRepository;
        this.bookRepository = bookRepository;
        this.cartRepository = cartRepository;
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
            customer.setCart(cart);
        }
        cart.addBook(book);
        cartRepository.save(cart);

        return "Book '" + book.getTitle() + "' added to " + customer.getName() + "'s cart";
    }

    @Transactional(readOnly = true)
    public double checkout(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Cart cart = customer.getCart();
        if (cart == null || cart.getBooks().isEmpty()) {
            return 0.0;
        }

        double total = cart.getBooks().stream().mapToDouble(Book::getPrice).sum();

        // Publish Kafka event por payment
        String event = String.format("Customer %s checked out with total=%.2f", customer.getName(), total);
        kafkaTemplate.send("checkout-events-topic", event);

        return total;
    }
}
