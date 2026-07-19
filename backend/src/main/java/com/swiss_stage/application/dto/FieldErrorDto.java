package com.swiss_stage.application.dto;

/** バリデーション・CSVインポートの詳細エラー(schema/openapi.yaml の FieldError) */
public record FieldErrorDto(String field, String reason) {}
