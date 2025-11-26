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

public class StoryCommentEdgeCaseTest {
    private static final Logger logger = LoggerFactory.getLogger(StoryCommentEdgeCaseTest.class);
    private HackerNewsClient client;

    @BeforeClass
    public void setup() {
        logger.info("Setting up Story-Comment Edge Case test suite");
        client = new HackerNewsClient();
    }

    @Test(description = "Test story with no comments (kids field is null)",
            priority = 1,
            retryAnalyzer = RetryAnalyzer.class)
    public void testStoryWithNoComments() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testStoryWithNoComments, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem storyWithNoComments = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(50, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();
                if ("story".equals(story.getType()) && story.getKids() == null) {
                    storyWithNoComments = story;
                    logger.info("Found story with no comments - ID: {}, Title: {}, RequestId: {}",
                            story.getId(), story.getTitle(), requestId);
                    break;
                }
            }
        }

        if (storyWithNoComments != null) {
            Assert.assertNull(storyWithNoComments.getKids(),
                    "Story should have null kids field. RequestId: " + requestId);
            Assert.assertEquals(storyWithNoComments.getType(), "story",
                    "Item should be a story. RequestId: " + requestId);
            logger.info("Successfully validated story with no comments, RequestId: {}", requestId);
        } else {
            logger.info("No story without comments found in top 50, RequestId: {}", requestId);
        }
    }

    @Test(description = "Test story with empty comments list (kids field is empty array)",
            priority = 2,
            retryAnalyzer = RetryAnalyzer.class)
    public void testStoryWithEmptyCommentsList() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testStoryWithEmptyCommentsList, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem storyWithEmptyComments = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(50, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();
                if ("story".equals(story.getType()) && story.getKids() != null && story.getKids().isEmpty()) {
                    storyWithEmptyComments = story;
                    logger.info("Found story with empty comments list - ID: {}, Title: {}, RequestId: {}",
                            story.getId(), story.getTitle(), requestId);
                    break;
                }
            }
        }

        if (storyWithEmptyComments != null) {
            Assert.assertTrue(storyWithEmptyComments.getKids().isEmpty(),
                    "Story should have empty kids list. RequestId: " + requestId);
            logger.info("Successfully validated story with empty comments list, RequestId: {}", requestId);
        } else {
            logger.info("No story with empty comments list found in top 50, RequestId: {}", requestId);
        }
    }

    @Test(description = "Test story with exactly one comment",
            priority = 3,
            retryAnalyzer = RetryAnalyzer.class)
    public void testStoryWithSingleComment() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testStoryWithSingleComment, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem storyWithOneComment = null;
        Long commentId = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(30, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();
                if ("story".equals(story.getType()) && story.getKids() != null && story.getKids().size() == 1) {
                    storyWithOneComment = story;
                    commentId = story.getKids().get(0);
                    logger.info("Found story with single comment - Story ID: {}, Comment ID: {}, RequestId: {}",
                            story.getId(), commentId, requestId);
                    break;
                }
            }
        }

        if (storyWithOneComment != null && commentId != null) {
            ApiResponse<HackerNewsItem> commentResponse = client.getItem(commentId, requestId);

            Assert.assertTrue(commentResponse.isSuccess(),
                    "Should fetch comment successfully. RequestId: " + requestId);
            Assert.assertNotNull(commentResponse.getBody(),
                    "Comment should not be null. RequestId: " + requestId);

            HackerNewsItem comment = commentResponse.getBody();
            Assert.assertEquals(comment.getType(), "comment",
                    "Item should be a comment. RequestId: " + requestId);
            Assert.assertEquals(comment.getParent(), storyWithOneComment.getId(),
                    "Comment parent should match story ID. RequestId: " + requestId);

            logger.info("Successfully validated story with single comment, RequestId: {}", requestId);
        } else {
            logger.info("No story with exactly one comment found in top 30, RequestId: {}", requestId);
        }
    }

    @Test(description = "Test first comment is deleted/dead",
            priority = 4,
            retryAnalyzer = RetryAnalyzer.class)
    public void testStoryWithDeletedFirstComment() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testStoryWithDeletedFirstComment, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem storyWithDeletedComment = null;
        Long deletedCommentId = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(50, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();

                if ("story".equals(story.getType()) && story.getKids() != null && !story.getKids().isEmpty()) {
                    Long firstCommentId = story.getKids().get(0);
                    ApiResponse<HackerNewsItem> commentResponse = client.getItem(firstCommentId, requestId);

                    if (commentResponse.isSuccess() && commentResponse.getBody() != null) {
                        HackerNewsItem comment = commentResponse.getBody();
                        if (Boolean.TRUE.equals(comment.getDeleted()) || Boolean.TRUE.equals(comment.getDead())) {
                            storyWithDeletedComment = story;
                            deletedCommentId = firstCommentId;
                            logger.info("Found story with deleted/dead first comment - Story: {}, Comment: {}, RequestId: {}",
                                    story.getId(), firstCommentId, requestId);
                            break;
                        }
                    }
                }
            }
        }

        if (storyWithDeletedComment != null) {
            Assert.assertNotNull(deletedCommentId,
                    "Should have found deleted comment ID. RequestId: " + requestId);
            logger.info("Successfully found and validated deleted first comment, RequestId: {}", requestId);
        } else {
            logger.info("No story with deleted first comment found in top 50, RequestId: {}", requestId);
        }
    }

    @Test(description = "Test comment with nested replies (comment has kids)",
            priority = 5,
            retryAnalyzer = RetryAnalyzer.class)
    public void testFirstCommentWithReplies() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testFirstCommentWithReplies, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem commentWithReplies = null;
        HackerNewsItem parentStory = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(20, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();

                if ("story".equals(story.getType()) && story.getKids() != null && !story.getKids().isEmpty()) {
                    Long firstCommentId = story.getKids().get(0);
                    ApiResponse<HackerNewsItem> commentResponse = client.getItem(firstCommentId, requestId);

                    if (commentResponse.isSuccess() && commentResponse.getBody() != null) {
                        HackerNewsItem comment = commentResponse.getBody();
                        if (comment.getKids() != null && !comment.getKids().isEmpty()) {
                            commentWithReplies = comment;
                            parentStory = story;
                            logger.info("Found first comment with replies - Story: {}, Comment: {}, Replies: {}, RequestId: {}",
                                    story.getId(), comment.getId(), comment.getKids().size(), requestId);
                            break;
                        }
                    }
                }
            }
        }

        if (commentWithReplies != null) {
            SoftAssert softAssert = new SoftAssert();
            softAssert.assertEquals(commentWithReplies.getType(), "comment",
                    "Should be a comment. RequestId: " + requestId);
            softAssert.assertEquals(commentWithReplies.getParent(), parentStory.getId(),
                    "Comment parent should match story. RequestId: " + requestId);
            softAssert.assertNotNull(commentWithReplies.getKids(),
                    "Comment should have kids. RequestId: " + requestId);
            softAssert.assertTrue(commentWithReplies.getKids().size() > 0,
                    "Comment should have at least one reply. RequestId: " + requestId);

            logger.info("Successfully validated first comment with {} replies, RequestId: {}",
                    commentWithReplies.getKids().size(), requestId);
            softAssert.assertAll();
        } else {
            logger.info("No first comment with replies found in top 20, RequestId: {}", requestId);
        }
    }

    @Test(description = "Test story with many comments (high volume)",
            priority = 6,
            retryAnalyzer = RetryAnalyzer.class)
    public void testStoryWithManyComments() {
        String requestId = HackerNewsClient.generateRequestId();
        int minComments = 50;
        logger.info("Starting testStoryWithManyComments (min: {}), RequestId: {}", minComments, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem storyWithManyComments = null;
        Long firstCommentId = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(20, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();

                if ("story".equals(story.getType()) && story.getKids() != null && story.getKids().size() >= minComments) {
                    storyWithManyComments = story;
                    firstCommentId = story.getKids().get(0);
                    logger.info("Found story with many comments - ID: {}, Comments: {}, RequestId: {}",
                            story.getId(), story.getKids().size(), requestId);
                    break;
                }
            }
        }

        if (storyWithManyComments != null && firstCommentId != null) {
            ApiResponse<HackerNewsItem> commentResponse = client.getItem(firstCommentId, requestId);

            SoftAssert softAssert = new SoftAssert();
            softAssert.assertTrue(commentResponse.isSuccess(),
                    "Should fetch first comment successfully. RequestId: " + requestId);
            softAssert.assertNotNull(commentResponse.getBody(),
                    "First comment should not be null. RequestId: " + requestId);

            if (commentResponse.getBody() != null) {
                softAssert.assertEquals(commentResponse.getBody().getType(), "comment",
                        "Should be a comment. RequestId: " + requestId);
                softAssert.assertEquals(commentResponse.getBody().getParent(), storyWithManyComments.getId(),
                        "Comment parent should match story. RequestId: " + requestId);
            }

            logger.info("Successfully validated first comment of story with {} total comments, RequestId: {}",
                    storyWithManyComments.getKids().size(), requestId);
            softAssert.assertAll();
        } else {
            logger.info("No story with {} or more comments found in top 20, RequestId: {}", minComments, requestId);
        }
    }

    @DataProvider(name = "commentIndexProvider")
    public Object[][] commentIndexProvider() {
        return new Object[][] {
                {0, "First comment (index 0)"},
                {1, "Second comment (index 1)"},
                {2, "Third comment (index 2)"},
                {4, "Fifth comment (index 4)"}
        };
    }

    @Test(dataProvider = "commentIndexProvider",
            description = "Test fetching comments at different indices",
            priority = 7,
            retryAnalyzer = RetryAnalyzer.class)
    public void testCommentAtDifferentIndices(int commentIndex, String description) {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testCommentAtDifferentIndices - {}, RequestId: {}", description, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem story = null;
        Long commentId = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(20, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem tempStory = storyResponse.getBody();

                if ("story".equals(tempStory.getType()) && tempStory.getKids() != null && tempStory.getKids().size() > commentIndex) {
                    story = tempStory;
                    commentId = tempStory.getKids().get(commentIndex);
                    logger.info("Found story with {} - Story ID: {}, Comment ID: {}, RequestId: {}",
                            description, story.getId(), commentId, requestId);
                    break;
                }
            }
        }

        if (story != null && commentId != null) {
            ApiResponse<HackerNewsItem> commentResponse = client.getItem(commentId, requestId);

            Assert.assertTrue(commentResponse.isSuccess(),
                    "Should fetch comment successfully. RequestId: " + requestId);
            Assert.assertNotNull(commentResponse.getBody(),
                    "Comment should not be null. RequestId: " + requestId);
            Assert.assertEquals(commentResponse.getBody().getType(), "comment",
                    "Should be a comment. RequestId: " + requestId);
            Assert.assertEquals(commentResponse.getBody().getParent(), story.getId(),
                    "Comment parent should match story. RequestId: " + requestId);

            logger.info("Successfully validated {} for story {}, RequestId: {}",
                    description, story.getId(), requestId);
        } else {
            logger.info("No story found with comment at index {}, RequestId: {}", commentIndex, requestId);
        }
    }

    @Test(description = "Test comment without text content",
            priority = 8,
            retryAnalyzer = RetryAnalyzer.class)
    public void testCommentWithoutText() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testCommentWithoutText, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem commentWithoutText = null;
        HackerNewsItem parentStory = null;

        for (Long storyId : topStoriesResponse.getBody().subList(0, Math.min(30, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();

                if ("story".equals(story.getType()) && story.getKids() != null && !story.getKids().isEmpty()) {
                    for (Long commentId : story.getKids().subList(0, Math.min(5, story.getKids().size()))) {
                        ApiResponse<HackerNewsItem> commentResponse = client.getItem(commentId, requestId);

                        if (commentResponse.isSuccess() && commentResponse.getBody() != null) {
                            HackerNewsItem comment = commentResponse.getBody();
                            if (comment.getText() == null || comment.getText().trim().isEmpty()) {
                                commentWithoutText = comment;
                                parentStory = story;
                                logger.info("Found comment without text - Comment ID: {}, Story ID: {}, RequestId: {}",
                                        comment.getId(), story.getId(), requestId);
                                break;
                            }
                        }
                    }

                    if (commentWithoutText != null) {
                        break;
                    }
                }
            }
        }

        if (commentWithoutText != null) {
            Assert.assertEquals(commentWithoutText.getType(), "comment",
                    "Should be a comment. RequestId: " + requestId);
            Assert.assertEquals(commentWithoutText.getParent(), parentStory.getId(),
                    "Comment parent should match story. RequestId: " + requestId);
            logger.info("Successfully validated comment without text content, RequestId: {}", requestId);
        } else {
            logger.info("No comment without text found in top 30 stories, RequestId: {}", requestId);
        }
    }

    @Test(description = "Test all first comments of top 10 stories",
            priority = 9,
            retryAnalyzer = RetryAnalyzer.class)
    public void testAllFirstCommentsOfTopStories() {
        String requestId = HackerNewsClient.generateRequestId();
        int storiesToCheck = 10;
        logger.info("Starting testAllFirstCommentsOfTopStories (top {}), RequestId: {}", storiesToCheck, requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        List<Long> topStories = topStoriesResponse.getBody().subList(0, Math.min(storiesToCheck, topStoriesResponse.getBody().size()));
        int storiesWithComments = 0;
        int successfulCommentFetches = 0;

        for (Long storyId : topStories) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(storyId, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem story = storyResponse.getBody();

                if ("story".equals(story.getType()) && story.getKids() != null && !story.getKids().isEmpty()) {
                    storiesWithComments++;
                    Long firstCommentId = story.getKids().get(0);

                    ApiResponse<HackerNewsItem> commentResponse = client.getItem(firstCommentId, requestId);

                    if (commentResponse.isSuccess() && commentResponse.getBody() != null) {
                        successfulCommentFetches++;
                        logger.info("Story {}: First comment {} fetched successfully, RequestId: {}",
                                story.getId(), firstCommentId, requestId);
                    }
                }
            }
        }

        logger.info("Results: {}/{} stories had comments, {}/{} first comments fetched successfully, RequestId: {}",
                storiesWithComments, storiesToCheck, successfulCommentFetches, storiesWithComments, requestId);

        Assert.assertTrue(storiesWithComments > 0,
                "At least one story should have comments. RequestId: " + requestId);

        if (storiesWithComments > 0) {
            double successRate = (double) successfulCommentFetches / storiesWithComments;
            Assert.assertTrue(successRate >= 0.8,
                    "At least 80% of first comments should be fetched successfully. RequestId: " + requestId);
        }
    }

    @Test(description = "Test comment parent-child relationship validation",
            priority = 10,
            retryAnalyzer = RetryAnalyzer.class)
    public void testCommentParentRelationship() {
        String requestId = HackerNewsClient.generateRequestId();
        logger.info("Starting testCommentParentRelationship, RequestId: {}", requestId);

        ApiResponse<List<Long>> topStoriesResponse = client.getTopStories(requestId);
        Assert.assertTrue(topStoriesResponse.isSuccess(),
                "Failed to fetch top stories. RequestId: " + requestId);

        HackerNewsItem story = null;
        Long storyId = null;

        for (Long id : topStoriesResponse.getBody().subList(0, Math.min(10, topStoriesResponse.getBody().size()))) {
            ApiResponse<HackerNewsItem> storyResponse = client.getItem(id, requestId);

            if (storyResponse.isSuccess() && storyResponse.getBody() != null) {
                HackerNewsItem item = storyResponse.getBody();
                if ("story".equals(item.getType())) {
                    story = item;
                    storyId = id;
                    break;
                }
            }
        }

        Assert.assertNotNull(story, "Failed to find a valid story in top 10. RequestId: " + requestId);

        if (story.getKids() != null && !story.getKids().isEmpty()) {
            List<Long> commentIds = story.getKids();
            int validRelationships = 0;

            for (int i = 0; i < Math.min(5, commentIds.size()); i++) {
                Long commentId = commentIds.get(i);
                ApiResponse<HackerNewsItem> commentResponse = client.getItem(commentId, requestId);

                if (commentResponse.isSuccess() && commentResponse.getBody() != null) {
                    HackerNewsItem comment = commentResponse.getBody();

                    SoftAssert softAssert = new SoftAssert();
                    softAssert.assertEquals(comment.getType(), "comment",
                            "Item should be a comment. RequestId: " + requestId);
                    softAssert.assertEquals(comment.getParent(), storyId,
                            "Comment parent should match story ID. RequestId: " + requestId);
                    softAssert.assertTrue(story.getKids().contains(comment.getId()),
                            "Story kids should contain comment ID. RequestId: " + requestId);

                    if (comment.getType().equals("comment") && comment.getParent().equals(storyId)) {
                        validRelationships++;
                    }

                    logger.info("Comment {} relationship validated: parent={}, type={}, RequestId: {}",
                            comment.getId(), comment.getParent(), comment.getType(), requestId);
                    softAssert.assertAll();
                }
            }

            Assert.assertTrue(validRelationships > 0,
                    "At least one valid parent-child relationship should exist. RequestId: " + requestId);
            logger.info("Validated {} parent-child relationships for story {}, RequestId: {}",
                    validRelationships, storyId, requestId);
        } else {
            logger.info("Story {} has no comments to validate relationships, RequestId: {}", storyId, requestId);
        }
    }

    @AfterClass
    public void teardown() {
        logger.info("Story-Comment Edge Case test suite completed");
    }
}