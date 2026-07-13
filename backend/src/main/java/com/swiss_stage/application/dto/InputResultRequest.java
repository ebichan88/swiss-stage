package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.MatchResult;
import jakarta.validation.constraints.NotNull;

/** 対局結果入力(PUT・べき等)。versionは楽観ロック用 */
public record InputResultRequest(
        @NotNull(message = "結果は必須です")
        MatchResult result,

        @NotNull(message = "versionは必須です")
        Long version) {}
