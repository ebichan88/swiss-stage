package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentInvite;
import java.time.Instant;

final class TournamentInviteItemMapper {

  private TournamentInviteItemMapper() {}

  static TournamentInviteItem toItem(TournamentInvite invite) {
    var item = new TournamentInviteItem();
    item.setPk(DynamoDbKeys.pk(invite.tournamentId()));
    item.setSk(DynamoDbKeys.INVITE_SK);
    item.setEntityType(TournamentInviteItem.ENTITY_TYPE);
    item.setToken(invite.token());
    item.setExpiresAt(invite.expiresAt().toString());
    item.setMaxUses(invite.maxUses());
    item.setUsedCount(invite.usedCount());
    item.setCreatedAt(invite.createdAt().toString());
    item.setGsi2Pk(DynamoDbKeys.gsi2PkForInvite(invite.token()));
    // version 0 = 未保存。nullにするとEnhanced Clientが新規条件(attribute_not_exists)で書き込む
    item.setVersion(invite.version() == 0 ? null : invite.version());
    return item;
  }

  static TournamentInvite toDomain(TournamentInviteItem item) {
    return new TournamentInvite(
        new TournamentId(item.getPk().substring("TOURNAMENT#".length())),
        item.getToken(),
        Instant.parse(item.getExpiresAt()),
        item.getMaxUses(),
        item.getUsedCount(),
        item.getVersion() == null ? 0L : item.getVersion(),
        Instant.parse(item.getCreatedAt()));
  }
}
