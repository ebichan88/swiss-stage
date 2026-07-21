package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.MatchSide;
import jakarta.validation.constraints.NotNull;

/** トークン経由の結果自己申告。versionは楽観ロック用 */
public record ReportMatchResultRequest(
        @NotNull(message = "reportedByは必須です")
        MatchSide reportedBy,

        @NotNull(message = "結果は必須です")
        MatchResult result,

        @NotNull(message = "versionは必須です")
        Long version) {}
