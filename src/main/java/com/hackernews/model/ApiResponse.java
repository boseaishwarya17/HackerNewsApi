package com.hackernews.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
    private int statusCode;
    private T body;
    private long responseTime;
    private String requestId;
    private boolean isSuccess;
    private String errorMessage;
}