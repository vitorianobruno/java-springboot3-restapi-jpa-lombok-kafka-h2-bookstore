package com.project.bookstore.repository;

import com.project.bookstore.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM cart_books WHERE cart_id = :cartId", nativeQuery = true)
    void clearCartBooks(@Param("cartId") Long cartId);
}