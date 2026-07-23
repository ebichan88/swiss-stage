package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.MatchResult;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 対局のボード結果一括入力(PUT・べき等)。boardResultsはteamSizeの長さで、
 * 各要素はNONE(未入力のまま)またはPLAYER1_WIN/PLAYER2_WIN/DRAW/BOTH_LOSE(BYEは400)。
 * versionは楽観ロック用。
 */
public record InputTeamMatchResultRequest(
        @NotEmpty(message = "boardResultsは必須です")
        List<MatchResult> boardResults,

        @NotNull(message = "versionは必須です")
        Long version) {}
