package com.revature.get_books;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BookService {

    private final S3Presigner presigner;

    public BookService() {
        presigner = S3Presigner.builder().region(Region.US_WEST_1).build();
    }

    public BookService(S3Presigner presigner) {
        this.presigner = presigner;
    }

    public List<BookResponse> mapResponse(PageIterable<Book> books, LambdaLogger logger) {

        logger.log("Mapping provided models to response. Provided models: " + books);

        if (books == null || !books.iterator().hasNext()) {
            return Collections.emptyList();
        }

        List<BookResponse> respBody = new ArrayList<>();

        logger.log("Books: " + books.stream());
        books.stream()
             .forEach(page -> page.items().forEach(book -> {

                 logger.log("Book: " + book);

                 String imageUrl = "";
                 if (book.getImageKey() != null && !book.getImageKey().isEmpty()) {
                     imageUrl = getPresignedImageUrl(book.getImageKey(), logger);
                 }

                 respBody.add(BookResponse.builder()
                                          .id(book.getId())
                                          .title(book.getTitle())
                                          .publisher(book.getPublisher())
                                          .authors(book.getAuthors())
                                          .genres(book.getGenres())
                                          .imageUrl(imageUrl)
                                          .build());
             }));

        logger.log("Returning mapped response body: " + respBody);

        return respBody;

    }

    private String getPresignedImageUrl(String imageKey, LambdaLogger logger) {

        logger.log("Fetching presigned URL for provided object key: " + imageKey);

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
