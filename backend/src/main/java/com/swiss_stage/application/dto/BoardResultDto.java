package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.BoardResult;
import com.swiss_stage.domain.model.MatchResult;

public record BoardResultDto(
        int boardPosition,
        MatchResult result,
        MatchResult team1ReportedResult,
        MatchResult team2ReportedResult) {

    public static BoardResultDto from(BoardResult b) {
        return new BoardResultDto(
                b.boardPosition(), b.result(), b.team1ReportedResult(), b.team2ReportedResult());
    }
}
