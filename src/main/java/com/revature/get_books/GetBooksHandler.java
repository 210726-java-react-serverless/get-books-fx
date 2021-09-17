package com.revature.get_books;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.SneakyThrows;

import java.time.Duration;
import java.util.*;

public class GetBooksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson mapper = new GsonBuilder().setPrettyPrinting().create();

    private final DynamoDbClient db = DynamoDbClient.builder().httpClient(ApacheHttpClient.create()).build();
    private final DynamoDbEnhancedClient dbClient = DynamoDbEnhancedClient.builder().dynamoDbClient(db).build();
    private final DynamoDbTable<Book> bookTable = dbClient.table("books", TableSchema.fromBean(Book.class));
    private final S3Presigner presigner = S3Presigner.builder().region(Region.US_WEST_1).build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {


        LambdaLogger logger = context.getLogger();
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();

        logger.log("Deployment successful!");

        Map<String, String> queryParams = requestEvent.getQueryStringParameters();

        PageIterable<Book> books;

        if (queryParams == null || queryParams.isEmpty()) {
            books = getAllBooks();
        } else {
            books = searchBooks(queryParams, logger);
        }

        List<BookResponse> respBody = new ArrayList<>();
        books.stream()
             .forEach(page -> page.items().forEach(book -> {
                                  BookResponse bookResp = new BookResponse();
                                  bookResp.setId(book.getId());
                                  bookResp.setTitle(book.getTitle());
                                  bookResp.setPublisher(book.getPublisher());
                                  bookResp.setAuthors(book.getAuthors());
                                  bookResp.setGenres(book.getGenres());
                                  if (book.getImageKey() != null && !book.getImageKey().isEmpty()) {
                                      bookResp.setImageUrl(getPresignedImageUrl(book.getImageKey(), logger));
                                  }
                                  respBody.add(bookResp);
             }));


        presigner.close();

        responseEvent.setBody(mapper.toJson(respBody));
        return responseEvent;

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
                throw new RuntimeException("The field, " + paramKey + ", was not found on resource type: Book");
            }

            String fieldType = Book.class.getDeclaredField(paramKey).getType().getSimpleName();
            String paramVal = Optional.ofNullable(queryParams.get(paramKey))
                                      .orElseThrow(() -> new RuntimeException("Unexpected null value found in parameter map."));


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

        logger.log("{\n" +
                "\t\"filterExpression\": \"" + filterExprBuilder + "\"," +
                "\t\"attributeValues\": \"" + attributeValues + "\"" +
                "\n}");

        Expression filterExpr = Expression.builder().expression(filterExprBuilder.toString()).expressionValues(attributeValues).build();

        ScanEnhancedRequest scanExpr = ScanEnhancedRequest.builder()
                                                 .filterExpression(filterExpr)
                                                 .build();

        return bookTable.scan(scanExpr);

    }

    public String getPresignedImageUrl(String imageKey, LambdaLogger logger) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket("bookstore-images-bucket")
                                                            .key(imageKey)
                                                            .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                                                                                 .signatureDuration(Duration.ofMinutes(10))
                                                                                 .getObjectRequest(getObjectRequest)
                                                                                 .build();

        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);

        String presignedUrl = presignedGetObjectRequest.url().toString();
        logger.log("Presigned URL: " + presignedUrl);

        return presignedUrl;
    }

}
