package com.project.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1 cart have many books
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    // One cart belongs to one customer
    @OneToOne
    @JoinColumn(name = "customer_id")   // Foreign key in Cart table
    private Customer customer;

    public void addBook(Book book) {
        books.add(book);
        book.setCart(this);
    }

    public void removeBook(Book book) {
        books.remove(book);
        book.setCart(null);
    }

}
