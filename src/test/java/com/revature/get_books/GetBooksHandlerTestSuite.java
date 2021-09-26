package com.revature.get_books;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.revature.get_books.stubs.TestLogger;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class GetBooksHandlerTestSuite {

    static TestLogger testLogger;
    static final Gson mapper = new GsonBuilder().setPrettyPrinting().create();

    GetBooksHandler sut;
    Context mockContext;
    BookRepository mockBookRepo;
    BookService mockBookService;
    BookResponse stubbedBookResponse;

    @BeforeAll
    public static void suiteSetUp() {
        testLogger = new TestLogger();
    }

    @BeforeEach
    public void caseSetUp() {
        mockBookRepo = mock(BookRepository.class);
        mockBookService = mock(BookService.class);
        sut = new GetBooksHandler(mockBookRepo, mockBookService);

        mockContext = mock(Context.class);
        when(mockContext.getLogger()).thenReturn(testLogger);

        stubbedBookResponse = BookResponse.builder()
                                      .id("123")
                                      .isbn("0123456789-123")
                                      .title("Test Book")
                                      .publisher("Revature")
                                      .authors(Arrays.asList("Test Author 1", "Test Author 2"))
                                      .genres(Arrays.asList("Test Genre 1", "Test Genre 2"))
                                      .imageUrl("https://test-book-cover-image-url")
                                      .build();
    }

    @AfterEach
    public void caseTearDown() {
        sut = null;
        reset(mockContext, mockBookRepo, mockBookService);
        stubbedBookResponse = null;
    }

    @AfterAll
    public static void suiteCleanUp() {
        testLogger.close();
    }

    @Test
    public void given_validRequest_handlerGetsAllBooks() {

        // Arrange
        APIGatewayProxyRequestEvent mockRequestEvent = new APIGatewayProxyRequestEvent();
        mockRequestEvent.withPath("/books");
        mockRequestEvent.withHttpMethod("GET");
        mockRequestEvent.withBody(null);
        mockRequestEvent.withQueryStringParameters(null);

        @SuppressWarnings("unchecked")
        PageIterable<Book> mockBooks = (PageIterable<Book>) mock(PageIterable.class);
        when(mockBookRepo.getAllBooks()).thenReturn(mockBooks);

        List<BookResponse> mockBookResponses = Collections.singletonList(stubbedBookResponse);
        when(mockBookService.mapResponse(mockBooks, testLogger)).thenReturn(mockBookResponses);

        APIGatewayProxyResponseEvent expectedResponse = new APIGatewayProxyResponseEvent();
        expectedResponse.setStatusCode(200);
        expectedResponse.setBody(mapper.toJson(mockBookResponses));

        // Act
        APIGatewayProxyResponseEvent actualResponse = sut.handleRequest(mockRequestEvent, mockContext);

        // Assert
        verify(mockBookRepo, times(1)).getAllBooks();
        verify(mockBookRepo, times(0)).searchBooks(Collections.emptyMap(), testLogger);
        verify(mockBookService, times(1)).mapResponse(mockBooks, testLogger);
        assertEquals(expectedResponse, actualResponse);

    }

    @Test
    public void given_validRequest_handlerSearchesForBookByIsbn() {

        // Arrange
        APIGatewayProxyRequestEvent mockRequestEvent = new APIGatewayProxyRequestEvent();
        mockRequestEvent.withPath("/books");
        mockRequestEvent.withHttpMethod("GET");
        mockRequestEvent.withBody(null);
        mockRequestEvent.withQueryStringParameters(Collections.singletonMap("isbn", "0123456789-123"));

        @SuppressWarnings("unchecked")
        PageIterable<Book> mockBooks = (PageIterable<Book>) mock(PageIterable.class);
        Map<String, String> stubbedQueryParams = mockRequestEvent.getQueryStringParameters();
        when(mockBookRepo.searchBooks(stubbedQueryParams, testLogger)).thenReturn(mockBooks);

        List<BookResponse> mockBookResponses = Collections.singletonList(stubbedBookResponse);
        when(mockBookService.mapResponse(mockBooks, testLogger)).thenReturn(mockBookResponses);

        APIGatewayProxyResponseEvent expectedResponse = new APIGatewayProxyResponseEvent();
        expectedResponse.setStatusCode(200);
        expectedResponse.setBody(mapper.toJson(mockBookResponses));

        // Act
        APIGatewayProxyResponseEvent actualResponse = sut.handleRequest(mockRequestEvent, mockContext);

        // Assert
        verify(mockBookRepo, times(0)).getAllBooks();
        verify(mockBookRepo, times(1)).searchBooks(stubbedQueryParams, testLogger);
        verify(mockBookService, times(1)).mapResponse(mockBooks, testLogger);
        assertEquals(expectedResponse, actualResponse);

    }

}
