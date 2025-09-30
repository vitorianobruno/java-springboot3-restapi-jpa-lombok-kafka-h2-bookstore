package com.project.bookstore.service;

import com.project.bookstore.model.Book;
import com.project.bookstore.model.Cart;
import com.project.bookstore.model.Customer;
import com.project.bookstore.repository.BookRepository;
import com.project.bookstore.repository.CartRepository;
import com.project.bookstore.repository.CustomerRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService {

    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;
    private final CartRepository cartRepository;

    public CartService(CustomerRepository customerRepository, BookRepository bookRepository, CartRepository cartRepository) {
        this.customerRepository = customerRepository;
        this.bookRepository = bookRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional
    public String addBookToCart(Long customerId, String bookTitle) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found."));

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
        return cart.getBooks().stream().mapToDouble(Book::getPrice).sum();
    }

}
