package com.revature.get_books;

import lombok.Data;

import java.util.List;

@Data
public class BookResponse {
    private String id;
    private String isbn;
    private String title;
    private String publisher;
    private List<String> authors;
    private List<String> genres;
    private String imageUrl;
}
