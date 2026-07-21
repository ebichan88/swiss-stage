package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.ResultInputBy;
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
        item.setResultInputBy(m.resultInputBy() == null ? null : m.resultInputBy().name());
        item.setPlayer1ReportedResult(m.player1ReportedResult().name());
        item.setPlayer2ReportedResult(m.player2ReportedResult().name());
        item.setVersion(m.version() == 0 ? null : m.version());
        item.setGroupId(m.groupId().value());
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
                item.getResultInputBy() == null ? null : ResultInputBy.valueOf(item.getResultInputBy()),
                item.getPlayer1ReportedResult() == null
                        ? MatchResult.NONE : MatchResult.valueOf(item.getPlayer1ReportedResult()),
                item.getPlayer2ReportedResult() == null
                        ? MatchResult.NONE : MatchResult.valueOf(item.getPlayer2ReportedResult()),
                item.getVersion() == null ? 0L : item.getVersion(),
                new GroupId(item.getGroupId()));
    }
}
