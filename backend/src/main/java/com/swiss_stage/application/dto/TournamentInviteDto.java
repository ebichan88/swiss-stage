package com.swiss_stage.application.dto;

/**
 * 招待リンクDTO(schema/openapi.yaml の TournamentMembersView.invite)。招待の発行・失効・承諾は
 * 別PRで実装するため、現時点ではこの型のインスタンスは生成されない(常にnull)。
 */
public record TournamentInviteDto(String token, String expiresAt, int maxUses, int remainingUses) {}
