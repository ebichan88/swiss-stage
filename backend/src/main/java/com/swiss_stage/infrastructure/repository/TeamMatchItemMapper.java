package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.BoardResult;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.ResultInputBy;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamMatchId;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;

final class TeamMatchItemMapper {

    private TeamMatchItemMapper() {}

    static TeamMatchItem toItem(TournamentId tournamentId, TeamMatch m) {
        var item = new TeamMatchItem();
        item.setPk(DynamoDbKeys.pk(tournamentId));
        item.setSk(DynamoDbKeys.teamMatchSk(m.roundNumber(), m.id()));
        item.setEntityType(TeamMatchItem.ENTITY_TYPE);
        item.setTeamMatchId(m.id().value());
        item.setRoundNumber(m.roundNumber());
        item.setTableNumber(m.tableNumber());
        item.setTeam1Id(m.team1Id().value());
        item.setTeam2Id(m.team2Id() == null ? null : m.team2Id().value());
        item.setBoardResults(m.boardResults().stream().map(TeamMatchItemMapper::toBoardItem).toList());
        item.setResultInputBy(m.resultInputBy() == null ? null : m.resultInputBy().name());
        item.setVersion(m.version() == 0 ? null : m.version());
        item.setGroupId(m.groupId().value());
        return item;
    }

    static TeamMatch toDomain(TeamMatchItem item) {
        List<BoardResult> boardResults = item.getBoardResults() == null
                ? List.of()
                : item.getBoardResults().stream().map(TeamMatchItemMapper::toBoardDomain).toList();
        return new TeamMatch(
                new TeamMatchId(item.getTeamMatchId()),
                item.getRoundNumber(),
                item.getTableNumber(),
                new TeamId(item.getTeam1Id()),
                item.getTeam2Id() == null ? null : new TeamId(item.getTeam2Id()),
                boardResults,
                item.getResultInputBy() == null ? null : ResultInputBy.valueOf(item.getResultInputBy()),
                item.getVersion() == null ? 0L : item.getVersion(),
                new GroupId(item.getGroupId()));
    }

    private static BoardResultItem toBoardItem(BoardResult b) {
        var boardItem = new BoardResultItem();
        boardItem.setBoardPosition(b.boardPosition());
        boardItem.setResult(b.result().name());
        boardItem.setTeam1ReportedResult(b.team1ReportedResult().name());
        boardItem.setTeam2ReportedResult(b.team2ReportedResult().name());
        return boardItem;
    }

    private static BoardResult toBoardDomain(BoardResultItem item) {
        return new BoardResult(
                item.getBoardPosition(),
                MatchResult.valueOf(item.getResult()),
                item.getTeam1ReportedResult() == null
                        ? MatchResult.NONE : MatchResult.valueOf(item.getTeam1ReportedResult()),
                item.getTeam2ReportedResult() == null
                        ? MatchResult.NONE : MatchResult.valueOf(item.getTeam2ReportedResult()));
    }
}
