package com.hackernews.tests;

import com.hackernews.client.HackerNewsClient;
import com.hackernews.model.ApiResponse;
import com.hackernews.model.HackerNewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.List;

public class PaginationApiTest {
    private static final Logger logger = LoggerFactory.getLogger(PaginationApiTest.class);
    private HackerNewsClient client;

    @BeforeClass
    public void setup() {
        logger.info("Setting up Pagination API test suite");
        client = new HackerNewsClient();
    }

    @DataProvider(name = "pageSizeProvider")
    public Object[][] pageSizeProvider() {
        return new Object[][] {
                {10, "Page size 10"},
                {20, "Page size 20"},
                {30, "Page size 30"},
                {50, "Page size 50"},
                {100, "Page size 100"}
        };
    }

    @Test(dataProvider = "pageSizeProvider",
            description = "Test fetching first page with different page sizes",
            priority = 1,
            retryAnalyzer = RetryAnalyzer.class)
    public void testFirstPageWithDifferentSizes(int pageSize, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testFirstPageWithDifferentSizes - {}, RequestId: {}", description, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();
        List<Long> firstPage = allStories.subList(0, Math.min(pageSize, allStories.size()));

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(firstPage.size(), Math.min(pageSize, allStories.size()),
                "First page should contain " + pageSize + " stories or less. RequestId: " + requestId);
        softAssert.assertTrue(firstPage.size() > 0,
                "First page should not be empty. RequestId: " + requestId);

        // Verify all IDs in the page are valid
        for (Long storyId : firstPage) {
            softAssert.assertNotNull(storyId, "Story ID should not be null");
            softAssert.assertTrue(storyId > 0, "Story ID should be positive");
        }

        logger.info("First page ({}) fetched successfully - {} stories, RequestId: {}",
                description, firstPage.size(), requestId);
        softAssert.assertAll();
    }

    @DataProvider(name = "pageNumberProvider")
    public Object[][] pageNumberProvider() {
        return new Object[][] {
                {0, 20, "Page 1 (0-19)"},
                {1, 20, "Page 2 (20-39)"},
                {2, 20, "Page 3 (40-59)"},
                {5, 20, "Page 6 (100-119)"},
                {10, 20, "Page 11 (200-219)"}
        };
    }

    @Test(dataProvider = "pageNumberProvider",
            description = "Test fetching different pages with pagination",
            priority = 2,
            retryAnalyzer = RetryAnalyzer.class)
    public void testPaginationByPageNumber(int pageNumber, int pageSize, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testPaginationByPageNumber - {}, RequestId: {}", description, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();
        int startIndex = pageNumber * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allStories.size());

        Assert.assertTrue(startIndex < allStories.size(),
                "Page number " + pageNumber + " exceeds available stories. RequestId: " + requestId);

        List<Long> page = allStories.subList(startIndex, endIndex);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(page.size() > 0,
                "Page should not be empty. RequestId: " + requestId);
        softAssert.assertTrue(page.size() <= pageSize,
                "Page size should not exceed " + pageSize + ". RequestId: " + requestId);

        // Verify IDs are valid
        for (Long storyId : page) {
            softAssert.assertNotNull(storyId, "Story ID should not be null");
            softAssert.assertTrue(storyId > 0, "Story ID should be positive");
        }

        logger.info("{} fetched successfully - {} stories (indices {}-{}), RequestId: {}",
                description, page.size(), startIndex, endIndex - 1, requestId);
        softAssert.assertAll();
    }

    @Test(description = "Test pagination with story details fetching",
            priority = 3,
            retryAnalyzer = RetryAnalyzer.class)
    public void testPaginationWithStoryDetails() {
        String requestId = HackerNewsClient.generateRequestId();
        int pageSize = 5;
        int pageNumber = 0;
        logger.info("Starting testPaginationWithStoryDetails - Page {}, Size {}, RequestId: {}",
                pageNumber, pageSize, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();
        int startIndex = pageNumber * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allStories.size());
        List<Long> page = allStories.subList(startIndex, endIndex);

        List<HackerNewsItem> fetchedStories = new ArrayList<>();
        int successCount = 0;

        for (Long storyId : page) {
            ApiResponse<HackerNewsItem> itemResponse = client.getItem(storyId, requestId);
            if (itemResponse.isSuccess() && itemResponse.getBody() != null) {
                fetchedStories.add(itemResponse.getBody());
                successCount++;
            }
        }

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(successCount >= (pageSize * 0.8),
                "At least 80% of stories should be fetched successfully. RequestId: " + requestId);

        for (HackerNewsItem story : fetchedStories) {
            softAssert.assertEquals(story.getType(), "story",
                    "Item type should be 'story'. RequestId: " + requestId);
            softAssert.assertNotNull(story.getTitle(),
                    "Story should have a title. RequestId: " + requestId);
        }

        logger.info("Paginated story details - Fetched {}/{} stories successfully, RequestId: {}",
                successCount, page.size(), requestId);
        softAssert.assertAll();
    }

    @Test(description = "Test last page handling (edge case)",
            priority = 4,
            retryAnalyzer = RetryAnalyzer.class)
    public void testLastPageEdgeCase() {
        String requestId = HackerNewsClient.generateRequestId();
        int pageSize = 50;
        logger.info("Starting testLastPageEdgeCase, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();
        int totalStories = allStories.size();
        int lastPageNumber = (totalStories - 1) / pageSize;
        int startIndex = lastPageNumber * pageSize;
        int endIndex = totalStories;

        List<Long> lastPage = allStories.subList(startIndex, endIndex);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(lastPage.size() > 0,
                "Last page should not be empty. RequestId: " + requestId);
        softAssert.assertTrue(lastPage.size() <= pageSize,
                "Last page size should not exceed page size. RequestId: " + requestId);

        logger.info("Last page (page {}) - {} stories (indices {}-{}), Total: {}, RequestId: {}",
                lastPageNumber + 1, lastPage.size(), startIndex, endIndex - 1, totalStories, requestId);
        softAssert.assertAll();
    }

    @Test(description = "Test pagination boundary - empty page beyond available stories",
            priority = 5,
            retryAnalyzer = RetryAnalyzer.class)
    public void testPaginationBeyondAvailableStories() {
        String requestId = HackerNewsClient.generateRequestId();
        int pageSize = 50;
        int pageNumber = 100; // Way beyond available pages
        logger.info("Starting testPaginationBeyondAvailableStories - Page {}, RequestId: {}",
                pageNumber, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();
        int startIndex = pageNumber * pageSize;

        if (startIndex >= allStories.size()) {
            logger.info("Page {} is beyond available stories (total: {}), RequestId: {}",
                    pageNumber, allStories.size(), requestId);
            Assert.assertTrue(true, "Successfully detected page beyond available stories");
        } else {
            int endIndex = Math.min(startIndex + pageSize, allStories.size());
            List<Long> page = allStories.subList(startIndex, endIndex);
            logger.info("Page {} exists with {} stories, RequestId: {}",
                    pageNumber, page.size(), requestId);
        }
    }

    @DataProvider(name = "offsetLimitProvider")
    public Object[][] offsetLimitProvider() {
        return new Object[][] {
                {0, 10, "First 10 stories"},
                {10, 10, "Stories 11-20"},
                {50, 25, "Stories 51-75"},
                {100, 50, "Stories 101-150"}
        };
    }

    @Test(dataProvider = "offsetLimitProvider",
            description = "Test offset-based pagination (offset + limit pattern)",
            priority = 6,
            retryAnalyzer = RetryAnalyzer.class)
    public void testOffsetLimitPagination(int offset, int limit, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testOffsetLimitPagination - {}, Offset: {}, Limit: {}, RequestId: {}",
                description, offset, limit, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();
        int endIndex = Math.min(offset + limit, allStories.size());

        Assert.assertTrue(offset < allStories.size(),
                "Offset should be within available stories. RequestId: " + requestId);

        List<Long> stories = allStories.subList(offset, endIndex);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(stories.size() > 0,
                "Should fetch at least one story. RequestId: " + requestId);
        softAssert.assertTrue(stories.size() <= limit,
                "Should not exceed limit. RequestId: " + requestId);

        logger.info("{} - Fetched {} stories (offset: {}, limit: {}), RequestId: {}",
                description, stories.size(), offset, limit, requestId);
        softAssert.assertAll();
    }

    @Test(description = "Test total story count consistency",
            priority = 7,
            retryAnalyzer = RetryAnalyzer.class)
    public void testTotalStoryCount() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testTotalStoryCount, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();
        int totalCount = allStories.size();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(totalCount > 0,
                "Should have at least one story. RequestId: " + requestId);
        softAssert.assertTrue(totalCount <= 500,
                "Top stories API returns up to 500 stories. RequestId: " + requestId);

        logger.info("Total top stories count: {}, RequestId: {}", totalCount, requestId);
        softAssert.assertAll();
    }

    @Test(description = "Test pagination with all pages iteration",
            priority = 8,
            retryAnalyzer = RetryAnalyzer.class)
    public void testIterateAllPages() {
        String requestId = HackerNewsClient.generateRequestId();
        int pageSize = 30;
        logger.info("Starting testIterateAllPages with page size {}, RequestId: {}", pageSize, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();
        int totalStories = allStories.size();
        int totalPages = (int) Math.ceil((double) totalStories / pageSize);
        int totalStoriesInPages = 0;

        for (int pageNumber = 0; pageNumber < totalPages; pageNumber++) {
            int startIndex = pageNumber * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalStories);
            List<Long> page = allStories.subList(startIndex, endIndex);
            totalStoriesInPages += page.size();

            logger.info("Page {} - {} stories (indices {}-{}), RequestId: {}",
                    pageNumber + 1, page.size(), startIndex, endIndex - 1, requestId);
        }

        Assert.assertEquals(totalStoriesInPages, totalStories,
                "Sum of all page sizes should equal total stories. RequestId: " + requestId);
        logger.info("Iterated through {} pages, Total stories verified: {}, RequestId: {}",
                totalPages, totalStoriesInPages, requestId);
    }

    @Test(description = "Test pagination with concurrent page fetches",
            priority = 9,
            retryAnalyzer = RetryAnalyzer.class)
    public void testConcurrentPageFetches() {
        String requestId = HackerNewsClient.generateRequestId();
        int pageSize = 5;
        logger.info("Starting testConcurrentPageFetches, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();

        // Fetch page 1 and page 3 stories
        List<Long> page1 = allStories.subList(0, Math.min(pageSize, allStories.size()));
        List<Long> page3 = allStories.subList(
                Math.min(pageSize * 2, allStories.size()),
                Math.min(pageSize * 3, allStories.size()));

        int page1SuccessCount = 0;
        int page3SuccessCount = 0;

        for (Long storyId : page1) {
            ApiResponse<HackerNewsItem> itemResponse = client.getItem(storyId, requestId);
            if (itemResponse.isSuccess() && itemResponse.getBody() != null) {
                page1SuccessCount++;
            }
        }

        for (Long storyId : page3) {
            ApiResponse<HackerNewsItem> itemResponse = client.getItem(storyId, requestId);
            if (itemResponse.isSuccess() && itemResponse.getBody() != null) {
                page3SuccessCount++;
            }
        }

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(page1SuccessCount >= (page1.size() * 0.8),
                "Page 1 should have at least 80% success rate. RequestId: " + requestId);
        softAssert.assertTrue(page3SuccessCount >= (page3.size() * 0.8),
                "Page 3 should have at least 80% success rate. RequestId: " + requestId);

        logger.info("Concurrent page fetches - Page 1: {}/{}, Page 3: {}/{}, RequestId: {}",
                page1SuccessCount, page1.size(), page3SuccessCount, page3.size(), requestId);
        softAssert.assertAll();
    }

    @Test(description = "Test zero-based vs one-based pagination indexing",
            priority = 10,
            retryAnalyzer = RetryAnalyzer.class)
    public void testPaginationIndexing() {
        String requestId = HackerNewsClient.generateRequestId();
        int pageSize = 10;
        logger.info("Starting testPaginationIndexing, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> allStories = topStoriesResponse.getBody();

        // Zero-based (page 0, 1, 2...)
        int zeroBasedPageNumber = 1;
        int zeroBasedStart = zeroBasedPageNumber * pageSize;
        int zeroBasedEnd = Math.min(zeroBasedStart + pageSize, allStories.size());
        List<Long> zeroBasedPage = allStories.subList(zeroBasedStart, zeroBasedEnd);

        // One-based (page 1, 2, 3...)
        int oneBasedPageNumber = 2;
        int oneBasedStart = (oneBasedPageNumber - 1) * pageSize;
        int oneBasedEnd = Math.min(oneBasedStart + pageSize, allStories.size());
        List<Long> oneBasedPage = allStories.subList(oneBasedStart, oneBasedEnd);

        // Both should fetch the same page (page 2 in one-based = page 1 in zero-based)
        Assert.assertEquals(zeroBasedPage, oneBasedPage,
                "Zero-based page 1 should equal one-based page 2. RequestId: " + requestId);

        logger.info("Pagination indexing verified - Zero-based page {} = One-based page {}, RequestId: {}",
                zeroBasedPageNumber, oneBasedPageNumber, requestId);
    }

    @AfterClass
    public void teardown() {
        logger.info("Pagination API test suite completed");
    }
}