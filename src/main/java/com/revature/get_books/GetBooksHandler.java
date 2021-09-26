package com.revature.get_books;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

public class GetBooksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson mapper = new GsonBuilder().setPrettyPrinting().create();

    private final BookRepository bookRepo;
    private final BookService bookService;

    public GetBooksHandler() {
        bookRepo = new BookRepository();
        bookService = new BookService();
    }

    public GetBooksHandler(BookRepository bookRepo, BookService bookService) {
        this.bookRepo = bookRepo;
        this.bookService = bookService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        LambdaLogger logger = context.getLogger();
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();

        logger.log("Deployment successful!");

        Map<String, String> queryParams = requestEvent.getQueryStringParameters();

        PageIterable<Book> books;

        if (queryParams == null || queryParams.isEmpty()) {
            books = bookRepo.getAllBooks();
        } else {
            books = bookRepo.searchBooks(queryParams, logger);
        }

        List<BookResponse> respBody = bookService.mapResponse(books, logger);
        responseEvent.setBody(mapper.toJson(respBody));
        responseEvent.setStatusCode(200);

        return responseEvent;

    }

}
