package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.GameType;

/** 招待のプレビュー(schema/openapi.yaml の InvitationPreview) */
public record InvitationPreviewDto(
    String tournamentId,
    String tournamentName,
    GameType gameType,
    CompetitionType competitionType,
    String eventDate,
    String expiresAt,
    boolean alreadyMember) {}
