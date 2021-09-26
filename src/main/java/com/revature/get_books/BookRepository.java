package com.revature.get_books;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import lombok.SneakyThrows;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

public class BookRepository {

    private final DynamoDbTable<Book> bookTable;

    public BookRepository() {
        DynamoDbClient db = DynamoDbClient.builder().httpClient(ApacheHttpClient.create()).build();
        DynamoDbEnhancedClient dbClient = DynamoDbEnhancedClient.builder().dynamoDbClient(db).build();
        bookTable = dbClient.table("books", TableSchema.fromBean(Book.class));
    }

    public BookRepository(DynamoDbTable<Book> bookTable) {
        this.bookTable = bookTable;
    }

    public PageIterable<Book> getAllBooks() {
        return bookTable.scan();
    }

    @SneakyThrows
    public PageIterable<Book> searchBooks(Map<String, String> queryParams, LambdaLogger logger) {



        StringBuilder filterExprBuilder = new StringBuilder();
        Map<String, AttributeValue> attributeValues = new HashMap<>();
        List<String> paramKeys = new ArrayList<>(queryParams.keySet());

        List<String> bookFieldNames = Book.getFieldNameStrings();

        for (int i = 0; i < paramKeys.size(); i++){

            filterExprBuilder.append("(");
            if (i != 0) filterExprBuilder.append(" and ");


            String paramKey = paramKeys.get(i);

            if (!bookFieldNames.contains(paramKey)) {
                String msg = "The field, " + paramKey + ", was not found on resource type: Book";
                logger.log(msg);
                throw new RuntimeException(msg);
            }

            String fieldType = Book.class.getDeclaredField(paramKey).getType().getSimpleName();
            String paramVal = Optional.ofNullable(queryParams.get(paramKey))
                                      .orElseThrow(() -> {
                                          String msg = "Unexpected null value found in parameter map.";
                                          logger.log(msg);
                                          return new RuntimeException(msg);
                                      });


            switch (fieldType) {
                case "String":

                    String attributeKeyVar = ":" + paramKey;
                    filterExprBuilder.append(paramKey).append(" = ").append(attributeKeyVar);
                    attributeValues.put(attributeKeyVar, AttributeValue.builder().s(paramVal).build());

                    break;

                case "List":

                    // If there is only one provided query value
                    if (!paramVal.contains(",")) {

                        String attrKey = ":" + paramKey;
                        filterExprBuilder.append("contains(").append(paramKey).append(",").append(attrKey).append(")");
                        attributeValues.put(attrKey, AttributeValue.builder().s(paramVal).build());

                    } else {

                        String[] listVals = paramVal.split(",");

                        for (int j = 0; j < listVals.length; j++) {
                            String key = ":" + paramKey + j;
                            filterExprBuilder.append("contains(").append(paramKey).append(",").append(key).append(")");
                            if (j != listVals.length - 1) filterExprBuilder.append(" or ");
                            attributeValues.put(key, AttributeValue.builder().s(listVals[j]).build());
                        }
                    }

                    break;
            }

            filterExprBuilder.append(")");

        }

        logger.log("Assembled filter expression with attribute values:\n{\n" +
                "\t\"filterExpression\": \"" + filterExprBuilder + "\"," +
                "\t\"attributeValues\": \"" + attributeValues + "\"" +
                "\n}");

        Expression filterExpr = Expression.builder().expression(filterExprBuilder.toString()).expressionValues(attributeValues).build();

        ScanEnhancedRequest scanExpr = ScanEnhancedRequest.builder()
                                                          .filterExpression(filterExpr)
                                                          .build();

        PageIterable<Book> books;

        try {
            books = bookTable.scan(scanExpr);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }

        logger.log("Table scan complete, result: " + books);

        return books;

    }

}
