package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.model.TournamentMemberId;
import com.swiss_stage.domain.model.TournamentRole;
import java.time.Instant;

final class TournamentMemberItemMapper {

  private TournamentMemberItemMapper() {}

  static TournamentMemberItem toItem(
      TournamentId tournamentId, TournamentMember member, Instant tournamentCreatedAt) {
    var item = new TournamentMemberItem();
    item.setPk(DynamoDbKeys.pk(tournamentId));
    item.setSk(DynamoDbKeys.memberSk(member.sub()));
    item.setEntityType(TournamentMemberItem.ENTITY_TYPE);
    item.setMemberId(member.id().value());
    item.setSub(member.sub());
    item.setRole(member.role().name());
    item.setDisplayName(member.displayName());
    item.setJoinedAt(member.joinedAt().toString());
    item.setGsi1Pk(DynamoDbKeys.gsi1Pk(member.sub()));
    item.setGsi1Sk(DynamoDbKeys.gsi1Sk(tournamentCreatedAt));
    return item;
  }

  static TournamentMember toDomain(TournamentMemberItem item) {
    return new TournamentMember(
        new TournamentMemberId(item.getMemberId()),
        item.getSub(),
        TournamentRole.valueOf(item.getRole()),
        item.getDisplayName(),
        Instant.parse(item.getJoinedAt()));
  }

  static TournamentId tournamentIdOf(TournamentMemberItem item) {
    return new TournamentId(item.getPk().substring("TOURNAMENT#".length()));
  }
}
