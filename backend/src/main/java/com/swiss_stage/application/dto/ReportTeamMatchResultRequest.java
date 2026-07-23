package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.MatchSide;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * トークン経由のボード単位自己申告。reportedBy は「自分がteam1/team2のどちらか」を表す。
 * 確定はボード単位で独立に行う(1ボードの不一致が他ボードの確定を妨げない)。versionは楽観ロック用
 */
public record ReportTeamMatchResultRequest(
        @NotNull(message = "reportedByは必須です")
        MatchSide reportedBy,

        @NotEmpty(message = "boardResultsは必須です")
        List<MatchResult> boardResults,

        @NotNull(message = "versionは必須です")
        Long version) {}
