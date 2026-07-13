package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.model.Visibility;

/** 大会DTO(07_type_definitions.md §3)。shareToken は運営者にのみ返す */
public record TournamentDto(
        String id,
        String name,
        GameType gameType,
        int totalRounds,
        int currentRound,
        TournamentStatus status,
        Visibility visibility,
        String shareToken,
        boolean resultInputEnabled,
        long version,
        String createdAt,
        String updatedAt) {

    public static TournamentDto from(Tournament t) {
        return new TournamentDto(
                t.id().value(), t.name(), t.gameType(), t.totalRounds(), t.currentRound(),
                t.status(), t.visibility(), t.shareToken(), t.resultInputEnabled(), t.version(),
                t.createdAt().toString(), t.updatedAt().toString());
    }
}
