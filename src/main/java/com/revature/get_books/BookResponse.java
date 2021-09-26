package com.revature.get_books;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private String id;
    private String isbn;
    private String title;
    private String publisher;
    private List<String> authors;
    private List<String> genres;
    private String imageUrl;
}
