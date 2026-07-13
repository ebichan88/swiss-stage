package com.swiss_stage.application.dto;

/** バリデーション・CSVインポートの詳細エラー(07_type_definitions.md ApiErrorBody.details) */
public record FieldErrorDto(String field, String reason) {}
