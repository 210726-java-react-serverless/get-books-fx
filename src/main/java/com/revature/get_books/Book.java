package com.revature.get_books;

import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DynamoDbBean
public class Book {

    private String id;
    private String isbn;
    private String title;
    private String publisher;
    private List<String> authors;
    private List<String> genres;
    private String imageKey;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public Book setId(String id) {
        this.id = id;
        return this;
    }

    @DynamoDbAttribute("isbn")
    public String getIsbn() {
        return isbn;
    }

    public Book setIsbn(String isbn) {
        this.isbn = isbn;
        return this;
    }

    @DynamoDbAttribute("title")
    public String getTitle() {
        return title;
    }

    public Book setTitle(String title) {
        this.title = title;
        return this;
    }

    @DynamoDbAttribute("publisher")
    public String getPublisher() {
        return publisher;
    }

    public Book setPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    @DynamoDbAttribute("authors")
    public List<String> getAuthors() {
        return authors;
    }

    public Book setAuthors(List<String> authors) {
        this.authors = authors;
        return this;
    }

    @DynamoDbAttribute("genres")
    public List<String> getGenres() {
        return genres;
    }

    public Book setGenres(List<String> genres) {
        this.genres = genres;
        return this;
    }

    @DynamoDbAttribute("imageKey")
    public String getImageKey() {
        return imageKey;
    }

    public Book setImageKey(String imageKey) {
        this.imageKey = imageKey;
        return this;
    }

    public static List<String> getFieldNameStrings() {
        return Stream.of(Book.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toList());
    }

}
