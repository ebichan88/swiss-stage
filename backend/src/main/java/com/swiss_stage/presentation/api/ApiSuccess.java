package com.swiss_stage.presentation.api;

import java.time.Instant;

/** 成功レスポンスの統一フォーマット(03_api_design.md §2) */
public record ApiSuccess<T>(boolean success, T data, Meta meta) {

    public record Meta(String timestamp) {}

    public static <T> ApiSuccess<T> of(T data, Instant now) {
        return new ApiSuccess<>(true, data, new Meta(now.toString()));
    }
}
