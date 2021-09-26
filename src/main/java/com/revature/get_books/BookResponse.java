package com.revature.get_books;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BookResponse {
    private String id;
    private String isbn;
    private String title;
    private String publisher;
    private List<String> authors;
    private List<String> genres;
    private String imageUrl;
}
