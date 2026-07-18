package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentStatus;
import java.util.List;

/**
 * 共有ページ(S10)向けの大会集約DTO(GET /api/v1/shared/{token})。
 * 共有トークン経由のレスポンスには shareToken・ownerSub を含めない(13_security_design.md §6)。
 */
public record SharedTournamentDto(
        SharedTournamentSummary tournament,
        List<RoundDto> rounds,
        List<GroupStandingsDto> standings) {

    public record SharedTournamentSummary(
            String name,
            GameType gameType,
            int totalRounds,
            int currentRound,
            TournamentStatus status,
            boolean resultInputEnabled) {

        public static SharedTournamentSummary from(Tournament t) {
            return new SharedTournamentSummary(
                    t.name(), t.gameType(), t.totalRounds(), t.currentRound(), t.status(),
                    t.resultInputEnabled());
        }
    }
}
