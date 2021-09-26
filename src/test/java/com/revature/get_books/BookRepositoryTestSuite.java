package com.revature.get_books;

import com.revature.get_books.stubs.TestLogger;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes"})
public class BookRepositoryTestSuite {

    static TestLogger testLogger;

    BookRepository sut;
    DynamoDbTable mockBookTable;

    @BeforeAll
    public static void suiteSetUp() {
        testLogger = new TestLogger();
    }

    @BeforeEach
    public void caseSetUp() {
        mockBookTable = mock(DynamoDbTable.class);
        sut = new BookRepository();
    }

    @AfterEach
    public void caseTearDown() {
        sut = null;
        reset(mockBookTable);
    }

    @AfterAll
    public static void suiteCleanUp() {
        testLogger.close();
    }

    @Test
    public void test_template() {
        fail();
    }
}
