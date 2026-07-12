package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.model.Visibility;
import java.time.Instant;

final class TournamentItemMapper {

    private TournamentItemMapper() {}

    static TournamentItem toItem(Tournament t) {
        var item = new TournamentItem();
        item.setPk(DynamoDbKeys.pk(t.id()));
        item.setSk(DynamoDbKeys.METADATA_SK);
        item.setEntityType(TournamentItem.ENTITY_TYPE);
        item.setName(t.name());
        item.setGameType(t.gameType().name());
        item.setTotalRounds(t.totalRounds());
        item.setCurrentRound(t.currentRound());
        item.setStatus(t.status().name());
        item.setVisibility(t.visibility().name());
        item.setShareToken(t.shareToken());
        item.setOwnerSub(t.ownerSub());
        item.setGsi1Pk(DynamoDbKeys.gsi1Pk(t.ownerSub()));
        item.setGsi1Sk(DynamoDbKeys.gsi1Sk(t.createdAt()));
        item.setGsi2Pk(t.shareToken() == null ? null : DynamoDbKeys.gsi2Pk(t.shareToken()));
        item.setCreatedAt(t.createdAt().toString());
        item.setUpdatedAt(t.updatedAt().toString());
        // version 0 = 未保存。nullにすると Enhanced Client が新規条件(attribute_not_exists)で書き込む
        item.setVersion(t.version() == 0 ? null : t.version());
        return item;
    }

    static Tournament toDomain(TournamentItem item) {
        return new Tournament(
                new TournamentId(item.getPk().substring("TOURNAMENT#".length())),
                item.getName(),
                GameType.valueOf(item.getGameType()),
                item.getTotalRounds(),
                item.getCurrentRound(),
                TournamentStatus.valueOf(item.getStatus()),
                Visibility.valueOf(item.getVisibility()),
                item.getShareToken(),
                item.getOwnerSub(),
                item.getVersion() == null ? 0L : item.getVersion(),
                Instant.parse(item.getCreatedAt()),
                Instant.parse(item.getUpdatedAt()));
    }
}
