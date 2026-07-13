package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.TournamentId;

final class MatchItemMapper {

    private MatchItemMapper() {}

    static MatchItem toItem(TournamentId tournamentId, Match m) {
        var item = new MatchItem();
        item.setPk(DynamoDbKeys.pk(tournamentId));
        item.setSk(DynamoDbKeys.matchSk(m.roundNumber(), m.id()));
        item.setEntityType(MatchItem.ENTITY_TYPE);
        item.setMatchId(m.id().value());
        item.setRoundNumber(m.roundNumber());
        item.setTableNumber(m.tableNumber());
        item.setPlayer1Id(m.player1Id().value());
        item.setPlayer2Id(m.player2Id() == null ? null : m.player2Id().value());
        item.setResult(m.result().name());
        item.setVersion(m.version() == 0 ? null : m.version());
        return item;
    }

    static Match toDomain(MatchItem item) {
        return new Match(
                new MatchId(item.getMatchId()),
                item.getRoundNumber(),
                item.getTableNumber(),
                new ParticipantId(item.getPlayer1Id()),
                item.getPlayer2Id() == null ? null : new ParticipantId(item.getPlayer2Id()),
                MatchResult.valueOf(item.getResult()),
                item.getVersion() == null ? 0L : item.getVersion());
    }
}
