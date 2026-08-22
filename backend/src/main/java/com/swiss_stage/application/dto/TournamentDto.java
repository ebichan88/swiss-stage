package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentRole;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.model.Visibility;

/**
 * 大会DTO(schema/openapi.yaml の Tournament)。shareTokenはOWNERにのみ返す
 * (共有URLの管理は大会設定=OWNER専用の機能であるため。14_tournament_collaboration.md §4.7)。
 */
public record TournamentDto(
    String id,
    String name,
    GameType gameType,
    CompetitionType competitionType,
    Integer teamSize,
    String eventDate,
    int totalRounds,
    int currentRound,
    TournamentStatus status,
    Visibility visibility,
    String shareToken,
    boolean resultInputEnabled,
    TournamentRole role,
    long version,
    String createdAt,
    String updatedAt) {

  public static TournamentDto from(Tournament t, TournamentRole role) {
    return new TournamentDto(
        t.id().value(),
        t.name(),
        t.gameType(),
        t.competitionType(),
        t.teamSize(),
        t.eventDate() == null ? null : t.eventDate().toString(),
        t.totalRounds(),
        t.currentRound(),
        t.status(),
        t.visibility(),
        role == TournamentRole.OWNER ? t.shareToken() : null,
        t.resultInputEnabled(),
        role,
        t.version(),
        t.createdAt().toString(),
        t.updatedAt().toString());
  }
}
