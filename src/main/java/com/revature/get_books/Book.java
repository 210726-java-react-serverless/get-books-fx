package com.revature.get_books;

import lombok.Builder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
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

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAttribute("isbn")
    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    @DynamoDbAttribute("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @DynamoDbAttribute("publisher")
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @DynamoDbAttribute("authors")
    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    @DynamoDbAttribute("genres")
    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    @DynamoDbAttribute("imageKey")
    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public static List<String> getFieldNameStrings() {
        return Stream.of(Book.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toList());
    }

}
