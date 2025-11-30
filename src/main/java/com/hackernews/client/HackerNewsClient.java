package com.hackernews.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackernews.model.ApiResponse;
import com.hackernews.model.HackerNewsItem;
import com.hackernews.model.RateLimitException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class HackerNewsClient {
    private static final Logger logger = LoggerFactory.getLogger(HackerNewsClient.class);
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0";
    private static final String TOP_STORY_URI = "/topstories.json";
    private static final String REQUEST_ID = "requestId";
    private final ObjectMapper objectMapper;
    private final Retry retry;

    public HackerNewsClient() {
        this.objectMapper = new ObjectMapper();
        this.retry = createRetryConfig();
        RestAssured.baseURI = BASE_URL;
    }

    private Retry createRetryConfig() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(500, 2))
                .retryOnException(e -> {
                    if (e instanceof RateLimitException) {
                        logger.warn("Rate limit hit, will retry after backoff");
                        return true;
                    }
                    return e instanceof RuntimeException &&
                            !(e instanceof IllegalArgumentException);
                })
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        Retry retry = Retry.of("hackerNewsApi", config);

        retry.getEventPublisher()
                .onRetry(event -> logger.warn("Retry attempt #{} for request. Reason: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getMessage()))
                .onSuccess(event -> logger.debug("Request succeeded after {} attempts",
                        event.getNumberOfRetryAttempts()))
                .onError(event -> logger.error("Request failed after {} attempts",
                        event.getNumberOfRetryAttempts()));

        return retry;
    }

    public ApiResponse<List<Long>> getTopStories(String requestId) {
        return executeWithRetry(() -> {
            setupMDC(requestId);
            logger.info("Fetching top stories with requestId: {}", requestId);

            long startTime = System.currentTimeMillis();
            Response response = RestAssured.given()
                    .header("X-Request-ID", requestId)
                    .get(TOP_STORY_URI);

            long responseTime = System.currentTimeMillis() - startTime;

            checkRateLimit(response);

            if (response.statusCode() == 200) {
                List<Long> stories = null;
                try {
                    stories = objectMapper.readValue(
                            response.asString(),
                            new TypeReference<List<Long>>() {});
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                logger.info("Successfully fetched {} top stories in {}ms",
                        stories.size(), responseTime);

                return ApiResponse.<List<Long>>builder()
                        .statusCode(response.statusCode())
                        .body(stories)
                        .responseTime(responseTime)
                        .requestId(requestId)
                        .isSuccess(true)
                        .build();
            } else {
                logger.error("Failed to fetch top stories. Status: {}", response.statusCode());
                return ApiResponse.<List<Long>>builder()
                        .statusCode(response.statusCode())
                        .responseTime(responseTime)
                        .requestId(requestId)
                        .isSuccess(false)
                        .errorMessage(response.asString())
                        .build();
            }
        }, requestId);
    }

    public ApiResponse<HackerNewsItem> getItem(Long itemId, String requestId) {
        return executeWithRetry(() -> {
            setupMDC(requestId);
            logger.info("Fetching item {} with requestId: {}", itemId, requestId);

            if (itemId == null || itemId <= 0) {
                throw new IllegalArgumentException("Invalid item ID: " + itemId);
            }

            long startTime = System.currentTimeMillis();
            Response response = RestAssured.given()
                    .header("X-Request-ID", requestId)
                    .get("/item/" + itemId + ".json");

            long responseTime = System.currentTimeMillis() - startTime;

            checkRateLimit(response);

            if (response.statusCode() == 200) {
                String responseBody = response.asString();

                if (responseBody == null || responseBody.trim().equals("null")) {
                    logger.warn("Item {} not found or deleted", itemId);
                    return ApiResponse.<HackerNewsItem>builder()
                            .statusCode(response.statusCode())
                            .responseTime(responseTime)
                            .requestId(requestId)
                            .isSuccess(false)
                            .errorMessage("Item not found or deleted")
                            .build();
                }

                HackerNewsItem item = null;
                try {
                    item = objectMapper.readValue(responseBody, HackerNewsItem.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                logger.info("Successfully fetched item {} (type: {}) in {}ms",
                        itemId, item.getType(), responseTime);

                return ApiResponse.<HackerNewsItem>builder()
                        .statusCode(response.statusCode())
                        .body(item)
                        .responseTime(responseTime)
                        .requestId(requestId)
                        .isSuccess(true)
                        .build();
            } else {
                logger.error("Failed to fetch item {}. Status: {}", itemId, response.statusCode());
                return ApiResponse.<HackerNewsItem>builder()
                        .statusCode(response.statusCode())
                        .responseTime(responseTime)
                        .requestId(requestId)
                        .isSuccess(false)
                        .errorMessage(response.asString())
                        .build();
            }
        }, requestId);
    }

    private <T> T executeWithRetry(Supplier<T> supplier, String requestId) {
        Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        try {
            return decoratedSupplier.get();
        } finally {
            MDC.clear();
        }
    }

    private void checkRateLimit(Response response) {
        if (response.statusCode() == 429) {
            String retryAfter = response.header("Retry-After");
            int retrySeconds = retryAfter != null ? Integer.parseInt(retryAfter) : 60;
            logger.error("Rate limit exceeded. Retry after {} seconds", retrySeconds);
            throw new RateLimitException("Rate limit exceeded", retrySeconds);
        }
    }

    private void setupMDC(String requestId) {
        MDC.put(REQUEST_ID, requestId);
    }

    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}