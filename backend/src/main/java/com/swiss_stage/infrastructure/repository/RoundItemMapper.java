package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.RoundStatus;
import com.swiss_stage.domain.model.TournamentId;

final class RoundItemMapper {

    private RoundItemMapper() {}

    static RoundItem toItem(TournamentId tournamentId, Round r) {
        var item = new RoundItem();
        item.setPk(DynamoDbKeys.pk(tournamentId));
        item.setSk(DynamoDbKeys.roundSk(r.roundNumber()));
        item.setEntityType(RoundItem.ENTITY_TYPE);
        item.setRoundNumber(r.roundNumber());
        item.setStatus(r.status().name());
        return item;
    }

    static Round toDomain(RoundItem item) {
        return new Round(item.getRoundNumber(), RoundStatus.valueOf(item.getStatus()));
    }
}
