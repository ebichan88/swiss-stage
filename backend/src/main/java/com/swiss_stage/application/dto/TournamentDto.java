package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentRole;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.model.Visibility;

/** 大会DTO(schema/openapi.yaml の Tournament)。shareToken は運営者にのみ返す */
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

  // 共同管理者(MAINTAINER)の仕組みが未実装のため、アクセスできる大会は常にOWNER
  public static TournamentDto from(Tournament t) {
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
        t.shareToken(),
        t.resultInputEnabled(),
        TournamentRole.OWNER,
        t.version(),
        t.createdAt().toString(),
        t.updatedAt().toString());
  }
}
