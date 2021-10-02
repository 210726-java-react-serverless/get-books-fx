package com.revature.get_books;

import com.revature.get_books.stubs.TestLogger;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BookServiceTestSuite {

    static TestLogger testLogger;

    BookService sut;
    S3Presigner mockPresigner;
    Page<Book> stubbedBookPage;

    @BeforeAll
    public static void suiteSetUp() {
        testLogger = new TestLogger();
    }

    @BeforeEach
    public void caseSetUp() {
        mockPresigner = mock(S3Presigner.class);
        sut = new BookService(mockPresigner);

        stubbedBookPage = Page.create(
                                    Collections.singletonList(
                                            new Book()
                                                .setId("123")
                                                .setIsbn("0123456789-123")
                                                .setTitle("Test Book")
                                                .setPublisher("Revature")
                                                .setAuthors(Arrays.asList("Test Author 1", "Test Author 2"))
                                                .setGenres(Arrays.asList("Test Genre 1", "Test Genre 2"))
                                                .setImageKey("test-book-cover-image-key")
                                        )
                                    );
    }

    @AfterEach
    public void caseCleanUp() {
        sut = null;
        reset(mockPresigner);
        stubbedBookPage = null;
    }

    @AfterAll
    public static void suiteCleanUp() {
        testLogger.close();
    }

    @Test
    public void given_nullIterable_returns_emptyList() {

        // Act
        List<BookResponse> actualResult = sut.mapResponse(null, testLogger);

        // Assert
        verify(mockPresigner, times(0)).presignGetObject((GetObjectPresignRequest) any());
        assertEquals(0, actualResult.size());
    }

    @Test
    public void given_emptyIterable_returns_emptyList() {

        // Arrange
        PageIterable<Book> mockBooks = mock(PageIterable.class);
        Iterator mockIterator = mock(Iterator.class);

        when(mockBooks.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(false);

        // Act
        List<BookResponse> actualResult = sut.mapResponse(mockBooks, testLogger);

        // Assert
        verify(mockPresigner, times(0)).presignGetObject((GetObjectPresignRequest) any());
        assertEquals(0, actualResult.size());
    }

    @Test
    public void given_validIterable_returns_populatedList() throws MalformedURLException {

        // Arrange
        Iterator stubbedIterator = new Iterator() {

            private int callCount = 0;

            @Override
            public boolean hasNext() {
                return (callCount == 0);
            }

            @Override
            public Object next() {
                callCount++;
                return stubbedBookPage;
            }
        };

        PageIterable<Book> mockBooks = () -> stubbedIterator;

        PresignedGetObjectRequest mockedPresignedRequest = mock(PresignedGetObjectRequest.class);
        URL stubbedUrl = new URL("https://stubbed-presigned-url.com");
        when(mockPresigner.presignGetObject((GetObjectPresignRequest) any())).thenReturn(mockedPresignedRequest);
        when(mockedPresignedRequest.url()).thenReturn(stubbedUrl);

        // Act
        List<BookResponse> actualResult = sut.mapResponse(mockBooks, testLogger);

        // Assert
        verify(mockPresigner, times(1)).presignGetObject((GetObjectPresignRequest) any());
        assertEquals(1, actualResult.size());
    }

}
