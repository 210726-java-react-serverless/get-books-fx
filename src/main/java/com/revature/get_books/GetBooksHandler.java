package com.revature.get_books;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Data;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetBooksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson mapper = new GsonBuilder().setPrettyPrinting().create();
    private final DynamoDBMapper dbReader = new DynamoDBMapper(AmazonDynamoDBClientBuilder.defaultClient());
    private final S3Presigner presigner = S3Presigner.builder().region(Region.of("us-west-1")).build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {


        LambdaLogger logger = context.getLogger();
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();

        logger.log("Deployment successful!");

        Map<String, String> queryParams = requestEvent.getQueryStringParameters();

        List<Book> books = new ArrayList<>();

        if (queryParams == null || queryParams.isEmpty()) {
            books = getAllBooks();
        } else {
            books = searchBooks(queryParams, logger);
        }

        List<BookResponse> respBody = books.stream().map(book -> {
                                                            BookResponse bookResp = new BookResponse();
                                                            bookResp.setId(book.id);
                                                            bookResp.setTitle(book.title);
                                                            bookResp.setPublisher(book.publisher);
                                                            bookResp.setAuthors(book.authors);
                                                            bookResp.setGenres(book.genres);
                                                            if (book.imageKey != null && !book.imageKey.isEmpty()) {
                                                                bookResp.setImageUrl(getPresignedImageUrl(book.imageKey, logger));
                                                            }
                                                            return bookResp;
                                                        }).collect(Collectors.toList());


        presigner.close();

        responseEvent.setBody(mapper.toJson(respBody));
        return responseEvent;

    }




    public List<Book> getAllBooks() {
        return dbReader.scan(Book.class, new DynamoDBScanExpression());
    }

    @SneakyThrows
    public List<Book> searchBooks(Map<String, String> queryParams, LambdaLogger logger) {

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
                    attributeValues.put(attributeKeyVar, new AttributeValue().withS(paramVal));

                    break;

                case "List":

                    // If there is only one provided query value
                    if (!paramVal.contains(",")) {

                        String attrKey = ":" + paramKey;
                        filterExprBuilder.append("contains(").append(paramKey).append(",").append(attrKey).append(")");
                        attributeValues.put(attrKey, new AttributeValue().withS(paramVal));

                    } else {

                        String[] listVals = paramVal.split(",");

                        for (int j = 0; j < listVals.length; j++) {
                            String key = ":" + paramKey + j;
                            filterExprBuilder.append("contains(").append(paramKey).append(",").append(key).append(")");
                            if (j != listVals.length - 1) filterExprBuilder.append(" or ");
                            attributeValues.put(key, new AttributeValue().withS(listVals[j]));
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

       DynamoDBScanExpression scanExpr = new DynamoDBScanExpression()
               .withFilterExpression(filterExprBuilder.toString())
               .withExpressionAttributeValues(attributeValues);

        return dbReader.scan(Book.class, scanExpr);

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

    @Data
    public static class BookResponse {
        private String id;
        private String isbn;
        private String title;
        private String publisher;
        private List<String> authors;
        private List<String> genres;
        private String imageUrl;
    }

    @Data
    @DynamoDBTable(tableName = "books")
    public static class Book {

        @DynamoDBHashKey
        @DynamoDBAutoGeneratedKey
        private String id;

        @DynamoDBAttribute
        private String isbn;

        @DynamoDBAttribute
        private String title;

        @DynamoDBAttribute
        private String publisher;

        @DynamoDBAttribute
        private List<String> authors;

        @DynamoDBAttribute
        private List<String> genres;

        @DynamoDBAttribute
        private String imageKey;

        public static List<String> getFieldNameStrings() {
            return Stream.of(Book.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toList());
        }

    }

}
