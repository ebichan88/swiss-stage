package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.model.TournamentRole;

/** 共同管理者DTO(schema/openapi.yaml の TournamentMember)。displayNameは個人情報 */
public record TournamentMemberDto(
    String memberId, String displayName, TournamentRole role, String joinedAt) {

  public static TournamentMemberDto from(TournamentMember member) {
    return new TournamentMemberDto(
        member.id().value(), member.displayName(), member.role(), member.joinedAt().toString());
  }
}
