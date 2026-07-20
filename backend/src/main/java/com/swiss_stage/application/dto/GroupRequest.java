package com.swiss_stage.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** グループ作成・改名のリクエスト(schema/openapi.yaml の GroupRequest) */
public record GroupRequest(
        @NotBlank(message = "グループ名は必須です")
        @Size(max = 50, message = "グループ名は50文字以内で入力してください")
        String name) {}
