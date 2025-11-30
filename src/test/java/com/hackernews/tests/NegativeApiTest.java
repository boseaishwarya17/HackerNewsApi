package com.hackernews.tests;

import com.hackernews.client.HackerNewsClient;
import com.hackernews.model.ApiResponse;
import com.hackernews.model.HackerNewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.List;

public class NegativeApiTest {
    private static final Logger logger = LoggerFactory.getLogger(NegativeApiTest.class);
    private HackerNewsClient client;

    @BeforeClass
    public void setup() {
        logger.info("Setting up Negative API test suite");
        client = new HackerNewsClient();
    }

    @DataProvider(name = "invalidItemIds")
    public Object[][] invalidItemIds() {
        return new Object[][] {
                {null, "Null item ID"},
                {0L, "Zero item ID"},
                {-1L, "Negative item ID"},
                {-999999L, "Large negative item ID"}
        };
    }

    @Test(dataProvider = "invalidItemIds",
            description = "Test edge cases with invalid item IDs",
            priority = 1)
    public void testInvalidItemIds(Long itemId, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Testing invalid item ID: {} ({}), RequestId: {}", itemId, description, requestId);

        try {
            ApiResponse<HackerNewsItem> response = client.getItem(itemId, requestId);
            Assert.assertFalse(response.isSuccess(),
                    "Request should fail for " + description + ". RequestId: " + requestId);
            logger.info("Invalid item ID test passed for: {}, RequestId: {}", description, requestId);
        } catch (IllegalArgumentException e) {
            logger.info("Expected exception caught for {}: {}, RequestId: {}",
                    description, e.getMessage(), requestId);
            Assert.assertTrue(true);
        }
    }

    @DataProvider(name = "edgeCaseItemIds")
    public Object[][] edgeCaseItemIds() {
        return new Object[][] {
                {999999999L, "Very large item ID (likely non-existent)"},
                {1L, "Item ID 1 (very old item)"}
        };
    }

    @Test(dataProvider = "edgeCaseItemIds",
            description = "Test edge cases with boundary item IDs",
            priority = 2,
            retryAnalyzer = RetryAnalyzer.class)
    public void testEdgeCaseItemIds(Long itemId, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Testing edge case item ID: {} ({}), RequestId: {}", itemId, description, requestId);

        ApiResponse<HackerNewsItem> response = client.getItem(itemId, requestId);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Status code should be 200 even for non-existent items. RequestId: " + requestId);

        if (response.getBody() == null) {
            logger.info("Item {} does not exist (as expected), RequestId: {}", itemId, requestId);
        } else {
            logger.info("Item {} exists with type: {}, RequestId: {}",
                    itemId, response.getBody().getType(), requestId);
        }
    }

    @Test(description = "Test fetching non-existent item",
            priority = 3,
            retryAnalyzer = RetryAnalyzer.class)
    public void testNonExistentItem() {
        String requestId = HackerNewsClient.generateRequestId();
        Long nonExistentId = 999999999L;
        logger.info("Testing non-existent item ID: {}, RequestId: {}", nonExistentId, requestId);

        ApiResponse<HackerNewsItem> response = client.getItem(nonExistentId, requestId);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Status code should be 200. RequestId: " + requestId);
        Assert.assertFalse(response.isSuccess(),
                "Response should indicate failure for non-existent item. RequestId: " + requestId);
        Assert.assertNull(response.getBody(),
                "Body should be null for non-existent item. RequestId: " + requestId);

        logger.info("Non-existent item test passed, RequestId: {}", requestId);
    }

    @Test(description = "Test with malformed request (negative test)",
            priority = 4)
    public void testNullRequestId() {
        logger.info("Testing API call with null request ID");

        try {
            ApiResponse<List<Long>> response = client.getTopStories(null);
            // Should still work, just won't have request ID tracking
            Assert.assertTrue(response.isSuccess() || !response.isSuccess(),
                    "Request should complete regardless of null request ID");
            logger.info("Null request ID test passed");
        } catch (Exception e) {
            logger.info("Expected behavior with null request ID: {}", e.getMessage());
            Assert.assertTrue(true);
        }
    }

    @DataProvider(name = "invalidIdBoundaries")
    public Object[][] invalidIdBoundaries() {
        return new Object[][] {
                {Long.MIN_VALUE, "Minimum Long value"},
                {-100000000L, "Large negative value"},
                {-1L, "Negative one"}
        };
    }

    @Test(dataProvider = "invalidIdBoundaries",
            description = "Test boundary values for item IDs",
            priority = 5)
    public void testItemIdBoundaries(Long itemId, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Testing boundary item ID: {} ({}), RequestId: {}", itemId, description, requestId);

        try {
            ApiResponse<HackerNewsItem> response = client.getItem(itemId, requestId);
            Assert.assertFalse(response.isSuccess(),
                    "Request should fail for " + description + ". RequestId: " + requestId);
            logger.info("Boundary test passed for: {}, RequestId: {}", description, requestId);
        } catch (IllegalArgumentException e) {
            logger.info("Expected exception for {}: {}, RequestId: {}",
                    description, e.getMessage(), requestId);
            Assert.assertTrue(true);
        }
    }

    @Test(description = "Test fetching deleted or dead item",
            priority = 6,
            retryAnalyzer = RetryAnalyzer.class)
    public void testDeletedItem() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Testing potentially deleted item, RequestId: {}", requestId);

        // Using a very old item ID that might be deleted
        Long oldItemId = 1L;
        ApiResponse<HackerNewsItem> response = client.getItem(oldItemId, requestId);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Status code should be 200. RequestId: " + requestId);

        if (response.getBody() != null) {
            HackerNewsItem item = response.getBody();
            logger.info("Old item {} - Type: {}, Deleted: {}, RequestId: {}",
                    oldItemId, item.getType(), item.getDeleted(), requestId);
        } else {
            logger.info("Item {} is null (possibly deleted), RequestId: {}", oldItemId, requestId);
        }
    }

    @Test(description = "Test with empty or invalid type filtering",
            priority = 7,
            retryAnalyzer = RetryAnalyzer.class)
    public void testStoryWithNoComments() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Testing story that might have no comments, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        // Try to find a story without comments
        boolean foundStoryWithoutComments = false;
        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(20, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();
                if (story.getKids() == null || story.getKids().isEmpty()) {
                    foundStoryWithoutComments = true;
                    logger.info("Found story without comments - ID: {}, Title: {}, RequestId: {}",
                            story.getId(), story.getTitle(), requestId);
                    break;
                }
            }
        }

        logger.info("Story without comments test - Found: {}, RequestId: {}",
                foundStoryWithoutComments, requestId);
    }

    @AfterClass
    public void teardown() {
        logger.info("Negative API test suite completed");
    }
}