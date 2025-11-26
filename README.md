# Requirements

It needs JAVA 17 to run

# Build the project
mvn clean install

# Run all tests
mvn test

# View logs
tail -f logs/hackernews-api-test.log

# Debug specific request
grep "request-id-here" logs/hackernews-api-test.log
```

## 🔍 **Debugging Example:**

When a test fails, you'll see:
```
[RequestId: a1b2c3d4-e5f6-7890] - Fetching top stories
[RequestId: a1b2c3d4-e5f6-7890] - Retry attempt #1
[RequestId: a1b2c3d4-e5f6-7890] - Successfully fetched 500 stories in 1234ms