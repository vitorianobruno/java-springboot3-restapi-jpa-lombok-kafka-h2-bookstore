package com.project.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDetailsDto {
    private Long customerId;
    private String customerName;
    private List<BookDto> books;
    private Double total;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookDto {
        private Long id;
        private String title;
        private Double price;
    }
}