package com.revature.get_books;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.revature.get_books.stubs.TestLogger;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class GetBooksHandlerTestSuite {

    static TestLogger testLogger;
    static final Gson mapper = new GsonBuilder().setPrettyPrinting().create();

    GetBooksHandler sut;
    Context mockContext;
    BookRepository mockBookRepo;
    BookService mockBookService;

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
    }

    @AfterEach
    public void caseTearDown() {
        sut = null;
        reset(mockContext, mockBookRepo, mockBookService);
    }

    @AfterAll
    public static void suiteCleanUp() {
        testLogger.close();
    }

    @Test
    public void given_validRequest_getAllBooks_returns_listOfBooks() {

        // Arrange
        APIGatewayProxyRequestEvent mockRequestEvent = new APIGatewayProxyRequestEvent();
        mockRequestEvent.withPath("/books");
        mockRequestEvent.withHttpMethod("GET");
        mockRequestEvent.withBody(null);
        mockRequestEvent.withQueryStringParameters(null);

        @SuppressWarnings("unchecked")
        PageIterable<Book> mockBooks = (PageIterable<Book>) mock(PageIterable.class);
        when(mockBookRepo.getAllBooks()).thenReturn(mockBooks);

        List<BookResponse> mockBookResponses = new ArrayList<>();
        mockBookResponses.add(new BookResponse("123",
                                               "0123456789-123",
                                               "Test Book",
                                               "Revature",
                                               Arrays.asList("Test Author 1", "Test Author 2"),
                                               Arrays.asList("Test Genre 1", "Test Genre 2"),
                                               "https://test-book-cover-image-url"));
        when(mockBookService.mapResponse(mockBooks, testLogger)).thenReturn(mockBookResponses);

        APIGatewayProxyResponseEvent expectedResponse = new APIGatewayProxyResponseEvent();
        expectedResponse.setStatusCode(200);
        expectedResponse.setBody(mapper.toJson(mockBookResponses));

        // Act
        APIGatewayProxyResponseEvent actualResponse = sut.handleRequest(mockRequestEvent, mockContext);

        // Assert
        verify(mockBookRepo, times(1)).getAllBooks();
//        verify(mockBookRepo, times(0)).searchBooks(any(), testLogger);
        verify(mockBookService, times(1)).mapResponse(mockBooks, testLogger);
        Assertions.assertEquals(expectedResponse, actualResponse);

    }

}
