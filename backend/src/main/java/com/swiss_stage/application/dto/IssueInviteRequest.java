package com.swiss_stage.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 招待リンクの発行・再発行リクエスト(schema/openapi.yaml の IssueInviteRequest) */
public record IssueInviteRequest(
    @NotNull(message = "人数枠は必須です")
        @Min(value = 1, message = "人数枠は1以上である必要があります")
        @Max(value = 9, message = "人数枠は9以下である必要があります")
        Integer maxUses) {}
