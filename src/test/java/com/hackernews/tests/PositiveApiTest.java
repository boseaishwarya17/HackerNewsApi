package com.hackernews.tests;

import com.hackernews.client.HackerNewsClient;
import com.hackernews.model.ApiResponse;
import com.hackernews.model.HackerNewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;

import java.util.List;

public class PositiveApiTest {
    private static final Logger logger = LoggerFactory.getLogger(PositiveApiTest.class);
    private HackerNewsClient client;

    @BeforeClass
    public void setup() {
        logger.info("Setting up Positive API test suite");
        client = new HackerNewsClient();
    }

    @Test(description = "Test retrieving top stories from HackerNews API",
            priority = 1,
            retryAnalyzer = RetryAnalyzer.class)
    public void testGetTopStories() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testGetTopStories with requestId: {}", requestId);

        ApiResponse<List<Long>> response = client.getTopStories(requestId);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(response.isSuccess(),
                "API call should be successful. RequestId: " + requestId);
        softAssert.assertEquals(response.getStatusCode(), 200,
                "Status code should be 200. RequestId: " + requestId);
        softAssert.assertNotNull(response.getBody(),
                "Response body should not be null. RequestId: " + requestId);
        softAssert.assertFalse(response.getBody().isEmpty(),
                "Top stories list should not be empty. RequestId: " + requestId);
        softAssert.assertTrue(response.getBody().size() > 0,
                "Should return at least one story. RequestId: " + requestId);
        softAssert.assertTrue(response.getResponseTime() < 5000,
                "Response time should be less than 5 seconds. RequestId: " + requestId);

        logger.info("Top stories count: {}, Response time: {}ms, RequestId: {}",
                response.getBody().size(), response.getResponseTime(), requestId);

        softAssert.assertAll();
    }

    @DataProvider(name = "storyIndexProvider")
    public Object[][] storyIndexProvider() {
        return new Object[][] {
                {0, "First top story"},
                {1, "Second top story"},
                {2, "Third top story"},
                {4, "Fifth top story"},
                {9, "Tenth top story"}
        };
    }

    @Test(dataProvider = "storyIndexProvider",
            description = "Test retrieving story details by index with data provider",
            priority = 2,
            retryAnalyzer = RetryAnalyzer.class)
    public void testGetStoryDetailsByIndex(int index, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testGetStoryDetailsByIndex for {} (index: {}), RequestId: {}",
                description, index, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);
        Assert.assertTrue(topStoriesResponse.getBody().size() > index,
                "Not enough stories to test index " + index + ". RequestId: " + requestId);

        Long storyId = topStoriesResponse.getBody().get(index);
        logger.info("{} ID: {}, RequestId: {}", description, storyId, requestId);

        ApiResponse<HackerNewsItem> itemResponse = client.getItem(storyId, requestId);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(itemResponse.isSuccess(),
                "API call should be successful for " + description + ". RequestId: " + requestId);
        softAssert.assertEquals(itemResponse.getStatusCode(), 200,
                "Status code should be 200. RequestId: " + requestId);
        softAssert.assertNotNull(itemResponse.getBody(),
                "Item should not be null. RequestId: " + requestId);

        HackerNewsItem item = itemResponse.getBody();
        if (item != null) {
            softAssert.assertEquals(item.getId(), storyId,
                    "Item ID should match requested ID. RequestId: " + requestId);
            softAssert.assertEquals(item.getType(), "story",
                    "Item type should be 'story'. RequestId: " + requestId);
            softAssert.assertNotNull(item.getTitle(),
                    "Story should have a title. RequestId: " + requestId);
            softAssert.assertNotNull(item.getBy(),
                    "Story should have an author. RequestId: " + requestId);

            logger.info("{} details - Title: {}, Author: {}, Score: {}, RequestId: {}",
                    description, item.getTitle(), item.getBy(), item.getScore(), requestId);
        }

        softAssert.assertAll();
    }

    @DataProvider(name = "itemTypeProvider")
    public Object[][] itemTypeProvider() {
        return new Object[][] {
                {"story", "Story validation"},
                {"comment", "Comment validation"}
        };
    }

    @Test(dataProvider = "itemTypeProvider",
            description = "Test retrieving different item types with validation",
            priority = 3,
            retryAnalyzer = RetryAnalyzer.class)
    public void testGetItemByType(String itemType, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testGetItemByType for {}, RequestId: {}", description, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem foundItem = null;
        Long itemId = null;

        if (itemType.equals("story")) {
            // Get first story
            itemId = topStoriesResponse.getBody().get(0);
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(itemId, requestId);
            if (storyResponse.isSuccess()) {
                foundItem = storyResponse.getBody();
            }
        } else if (itemType.equals("comment")) {
            // Find a story with comments and get first comment
            for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(10, topStoriesResponse.getBody().size()))) {
                ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

                if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                    HackerNewsItem story = storyResponse.getBody();
                    if (story.getKids() != null && !story.getKids().isEmpty()) {
                        itemId = story.getKids().get(0);
                        ApiResponse<HackerNewsItem> commentResponse = client.getItem(itemId, requestId);
                        if (commentResponse.isSuccess()) {
                            foundItem = commentResponse.getBody();
                            break;
                        }
                    }
                }
            }
        }

        Assert.assertNotNull(foundItem,
                "Should find " + itemType + ". RequestId: " + requestId);
        Assert.assertEquals(foundItem.getType(), itemType,
                "Item type should be " + itemType + ". RequestId: " + requestId);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(foundItem.getId(), "Item should have ID");
        softAssert.assertNotNull(foundItem.getBy(), "Item should have author");
        softAssert.assertNotNull(foundItem.getTime(), "Item should have timestamp");

        logger.info("{} validated - ID: {}, Type: {}, Author: {}, RequestId: {}",
                description, foundItem.getId(), foundItem.getType(), foundItem.getBy(), requestId);

        softAssert.assertAll();
    }

    @DataProvider(name = "responseTimeThresholds")
    public Object[][] responseTimeThresholds() {
        return new Object[][] {
                {5000, "Top stories under 5 seconds"},
                {3000, "Top stories under 3 seconds"},
                {10000, "Top stories under 10 seconds"}
        };
    }

    @Test(dataProvider = "responseTimeThresholds",
            description = "Test response time performance with different thresholds",
            priority = 4,
            retryAnalyzer = RetryAnalyzer.class)
    public void testResponseTimePerformance(int thresholdMs, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testResponseTimePerformance - {}, RequestId: {}", description, requestId);

        long startTime = System.currentTimeMillis();
        ApiResponse<List<Long>> response = client.getTopStories(requestId);
        long totalTime = System.currentTimeMillis() - startTime;

        Assert.assertTrue(response.isSuccess(),
                "API call should be successful. RequestId: " + requestId);
        Assert.assertTrue(totalTime < thresholdMs,
                "Response time should be under " + thresholdMs + "ms. Actual: " + totalTime + "ms. RequestId: " + requestId);

        logger.info("Performance test ({}) - Response time: {}ms (threshold: {}ms), RequestId: {}",
                description, totalTime, thresholdMs, requestId);
    }

    @DataProvider(name = "multipleStoriesCount")
    public Object[][] multipleStoriesCount() {
        return new Object[][] {
                {3, 2, "Fetch 3 stories, expect 2+ success"},
                {5, 4, "Fetch 5 stories, expect 4+ success"},
                {10, 8, "Fetch 10 stories, expect 8+ success"}
        };
    }

    @Test(dataProvider = "multipleStoriesCount",
            description = "Test fetching multiple stories with success threshold",
            priority = 5,
            retryAnalyzer = RetryAnalyzer.class)
    public void testMultipleStoryFetching(int storiesToFetch, int expectedSuccess, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testMultipleStoryFetching - {}, RequestId: {}", description, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> storyIds = topStoriesResponse.getBody().subList(0,
                Math.min(storiesToFetch, topStoriesResponse.getBody().size()));
        int successCount = 0;

        for (Long storyId : storyIds) {
            ApiResponse<HackerNewsItem> itemResponse = client.getItem(storyId, requestId);
            if (itemResponse.isSuccess() && itemResponse.getBody() != null) {
                successCount++;
            }
        }

        Assert.assertTrue(successCount >= expectedSuccess,
                description + " - Actual success: " + successCount + ". RequestId: " + requestId);
        logger.info("Successfully fetched {} out of {} stories (expected: {}+), RequestId: {}",
                successCount, storiesToFetch, expectedSuccess, requestId);
    }

    @DataProvider(name = "requiredFieldsProvider")
    public Object[][] requiredFieldsProvider() {
        return new Object[][] {
                {0, "First story required fields"},
                {1, "Second story required fields"},
                {2, "Third story required fields"}
        };
    }

    @Test(dataProvider = "requiredFieldsProvider",
            description = "Verify story contains required fields",
            priority = 6,
            retryAnalyzer = RetryAnalyzer.class)
    public void testStoryRequiredFields(int index, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testStoryRequiredFields - {}, RequestId: {}", description, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);
        Assert.assertTrue(topStoriesResponse.getBody().size() > index,
                "Not enough stories for index " + index + ". RequestId: " + requestId);

        Long storyId = topStoriesResponse.getBody().get(index);
        ApiResponse<HackerNewsItem> itemResponse = client.getItem(storyId, requestId);

        Assert.assertTrue(itemResponse.isSuccess(),
                "Failed to fetch story. RequestId: " + requestId);

        HackerNewsItem story = itemResponse.getBody();
        Assert.assertNotNull(story, "Story should not be null. RequestId: " + requestId);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(story.getId(), "Story ID should not be null");
        softAssert.assertNotNull(story.getType(), "Story type should not be null");
        softAssert.assertNotNull(story.getBy(), "Story author should not be null");
        softAssert.assertNotNull(story.getTime(), "Story timestamp should not be null");
        softAssert.assertNotNull(story.getTitle(), "Story title should not be null");

        logger.info("{} validated - ID: {}, Type: {}, Title: {}, RequestId: {}",
                description, story.getId(), story.getType(), story.getTitle(), requestId);

        softAssert.assertAll();
    }

    @DataProvider(name = "storyRangeProvider")
    public Object[][] storyRangeProvider() {
        return new Object[][] {
                {0, 5, "First 5 stories ID validation"},
                {5, 10, "Stories 6-10 ID validation"},
                {10, 15, "Stories 11-15 ID validation"}
        };
    }

    @Test(dataProvider = "storyRangeProvider",
            description = "Verify stories in range contain valid IDs",
            priority = 7,
            retryAnalyzer = RetryAnalyzer.class)
    public void testStoriesContainValidIds(int startIndex, int endIndex, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testStoriesContainValidIds - {}, RequestId: {}", description, requestId);

        ApiResponse<List<Long>> response = client.getTopStories(requestId);
        Assert.assertTrue(response.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> stories = response.getBody();
        int actualEndIndex = Math.min(endIndex, stories.size());

        SoftAssert softAssert = new SoftAssert();

        for (int i = startIndex; i < actualEndIndex; i++) {
            Long storyId = stories.get(i);
            softAssert.assertNotNull(storyId,
                    "Story ID at index " + i + " should not be null. RequestId: " + requestId);
            softAssert.assertTrue(storyId > 0,
                    "Story ID at index " + i + " should be positive. RequestId: " + requestId);
        }

        logger.info("Validated {} story IDs ({}), RequestId: {}",
                actualEndIndex - startIndex, description, requestId);
        softAssert.assertAll();
    }

    // ===============================================
    // EDGE CASE TESTS
    // ===============================================

    @Test(description = "Test fetching very first item in HackerNews history",
            priority = 8,
            retryAnalyzer = RetryAnalyzer.class)
    public void testEdgeCaseVeryFirstItem() {
        String requestId = HackerNewsClient.generateRequestId();
        Long firstItemId = 1L;
        logger.info("Starting testEdgeCaseVeryFirstItem - ID: {}, RequestId: {}", firstItemId, requestId);

        ApiResponse<HackerNewsItem> response = client.getItem(firstItemId, requestId);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Status code should be 200. RequestId: " + requestId);

        if (response.getBody() != null) {
            HackerNewsItem item = response.getBody();
            logger.info("First ever item exists - ID: {}, Type: {}, By: {}, RequestId: {}",
                    item.getId(), item.getType(), item.getBy(), requestId);

            Assert.assertEquals(item.getId(), firstItemId,
                    "Item ID should be 1. RequestId: " + requestId);
        } else {
            logger.info("First item (ID: 1) may have been deleted, RequestId: {}", requestId);
        }
    }

    @Test(description = "Test story with maximum comments",
            priority = 9,
            retryAnalyzer = RetryAnalyzer.class)
    public void testEdgeCaseStoryWithManyComments() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testEdgeCaseStoryWithManyComments, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem storyWithMostComments = null;
        int maxDescendants = 0;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(20, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();
                Integer descendants = story.getDescendants();

                if (descendants != null && descendants > maxDescendants) {
                    maxDescendants = descendants;
                    storyWithMostComments = story;
                }
            }
        }

        Assert.assertNotNull(storyWithMostComments,
                "Should find story with comments. RequestId: " + requestId);

        logger.info("Story with most comments - ID: {}, Title: {}, Descendants: {}, RequestId: {}",
                storyWithMostComments.getId(), storyWithMostComments.getTitle(),
                maxDescendants, requestId);

        Assert.assertTrue(maxDescendants >= 0,
                "Descendants count should be non-negative. RequestId: " + requestId);
    }

    @Test(description = "Test story with URL vs Ask HN story",
            priority = 10,
            retryAnalyzer = RetryAnalyzer.class)
    public void testEdgeCaseStoryWithAndWithoutURL() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testEdgeCaseStoryWithAndWithoutURL, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem storyWithURL = null;
        HackerNewsItem storyWithoutURL = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(30, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();

                if (story.getUrl() != null && storyWithURL == null) {
                    storyWithURL = story;
                }

                if (story.getUrl() == null && story.getText() != null && storyWithoutURL == null) {
                    storyWithoutURL = story;
                }

                if (storyWithURL != null && storyWithoutURL != null) {
                    break;
                }
            }
        }

        SoftAssert softAssert = new SoftAssert();

        if (storyWithURL != null) {
            softAssert.assertNotNull(storyWithURL.getUrl(), "Story with URL should have URL field");
            logger.info("Story with URL - ID: {}, Title: {}, URL: {}, RequestId: {}",
                    storyWithURL.getId(), storyWithURL.getTitle(), storyWithURL.getUrl(), requestId);
        }

        if (storyWithoutURL != null) {
            softAssert.assertNull(storyWithoutURL.getUrl(), "Ask HN story should not have URL");
            softAssert.assertNotNull(storyWithoutURL.getText(), "Ask HN story should have text");
            logger.info("Story without URL (Ask HN) - ID: {}, Title: {}, RequestId: {}",
                    storyWithoutURL.getId(), storyWithoutURL.getTitle(), requestId);
        }

        softAssert.assertAll();
    }

    @Test(description = "Test nested comments hierarchy",
            priority = 11,
            retryAnalyzer = RetryAnalyzer.class)
    public void testEdgeCaseNestedComments() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testEdgeCaseNestedComments, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem commentWithReplies = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(10, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();

                if (story.getKids() != null && !story.getKids().isEmpty()) {
                    for (Long commentId : story.getKids().subList(0, Math.min(3, story.getKids().size()))) {
                        ApiResponse<HackerNewsItem> commentResponse = client.getItem(commentId, requestId);

                        if (commentResponse.isSuccess() && commentResponse.getBody() != null) {
                            HackerNewsItem comment = commentResponse.getBody();

                            if (comment.getKids() != null && !comment.getKids().isEmpty()) {
                                commentWithReplies = comment;
                                break;
                            }
                        }
                    }

                    if (commentWithReplies != null) {
                        break;
                    }
                }
            }
        }

        if (commentWithReplies != null) {
            logger.info("Found nested comment - ID: {}, Parent: {}, Replies: {}, RequestId: {}",
                    commentWithReplies.getId(), commentWithReplies.getParent(),
                    commentWithReplies.getKids().size(), requestId);

            Assert.assertEquals(commentWithReplies.getType(), "comment",
                    "Item should be a comment. RequestId: " + requestId);
            Assert.assertNotNull(commentWithReplies.getKids(),
                    "Comment should have replies. RequestId: " + requestId);
            Assert.assertTrue(commentWithReplies.getKids().size() > 0,
                    "Comment should have at least one reply. RequestId: " + requestId);
        } else {
            logger.info("No nested comments found in top stories, RequestId: {}", requestId);
        }
    }

    @Test(description = "Test story score validation",
            priority = 12,
            retryAnalyzer = RetryAnalyzer.class)
    public void testEdgeCaseStoryScores() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testEdgeCaseStoryScores, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        SoftAssert softAssert = new SoftAssert();
        int storiesChecked = 0;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(10, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();

                if (story.getScore() != null) {
                    softAssert.assertTrue(story.getScore() >= 0,
                            "Story score should be non-negative. Story ID: " + story.getId() + ". RequestId: " + requestId);

                    logger.info("Story score - ID: {}, Title: {}, Score: {}, RequestId: {}",
                            story.getId(), story.getTitle(), story.getScore(), requestId);
                    storiesChecked++;
                }
            }
        }

        Assert.assertTrue(storiesChecked > 0,
                "Should check at least one story's score. RequestId: " + requestId);
        softAssert.assertAll();
    }

    @Test(description = "Test API consistency with duplicate requests",
            priority = 13,
            retryAnalyzer = RetryAnalyzer.class)
    public void testEdgeCaseAPIConsistency() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testEdgeCaseAPIConsistency, RequestId: {}", requestId);

        ApiResponse<List<Long>> firstResponse = client.getTopStories(requestId);
        Assert.assertTrue(firstResponse.isSuccess(),
                "First API call should be successful. RequestId: " + requestId);

        // Wait a bit and fetch again
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ApiResponse<List<Long>> secondResponse = client.getTopStories(requestId);
        Assert.assertTrue(secondResponse.isSuccess(),
                "Second API call should be successful. RequestId: " + requestId);

        // Top stories may change, but the list should still be valid
        Assert.assertNotNull(firstResponse.getBody(), "First response body should not be null");
        Assert.assertNotNull(secondResponse.getBody(), "Second response body should not be null");
        Assert.assertTrue(firstResponse.getBody().size() > 0, "First response should have stories");
        Assert.assertTrue(secondResponse.getBody().size() > 0, "Second response should have stories");

        logger.info("API consistency test - First call: {} stories, Second call: {} stories, RequestId: {}",
                firstResponse.getBody().size(), secondResponse.getBody().size(), requestId);
    }

    @AfterClass
    public void teardown() {
        logger.info("Positive API test suite completed");
    }
}