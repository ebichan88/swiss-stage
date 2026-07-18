package com.swiss_stage.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** グループ作成・改名のリクエスト(07_type_definitions.md の GroupInput) */
public record GroupRequest(
        @NotBlank(message = "グループ名は必須です")
        @Size(max = 50, message = "グループ名は50文字以内で入力してください")
        String name) {}
