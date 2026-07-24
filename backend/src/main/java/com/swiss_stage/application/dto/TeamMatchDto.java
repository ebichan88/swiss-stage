package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import java.util.List;
import java.util.Map;

/**
 * 団体戦対局DTO。team2 が null なら不戦勝(BYE)。
 * チーム全体の勝敗は保存せず、boardResultsから都度導出する(05_swiss_pairing_algorithm.md §5.3)。
 */
public record TeamMatchDto(
        String id,
        int roundNumber,
        int tableNumber,
        GroupDto group,
        TeamSummaryDto team1,
        TeamSummaryDto team2,
        List<BoardResultDto> boardResults,
        long version) {

    public static TeamMatchDto from(
            TeamMatch m, Map<TeamId, Team> teams, Map<GroupId, Group> groups) {
        Group group = groups.get(m.groupId());
        return new TeamMatchDto(
                m.id().value(),
                m.roundNumber(),
                m.tableNumber(),
                group == null
                        ? new GroupDto(m.groupId().value(), "(不明なグループ)")
                        : GroupDto.from(group),
                summaryOf(m.team1Id(), teams),
                m.team2Id() == null ? null : summaryOf(m.team2Id(), teams),
                m.boardResults().stream().map(BoardResultDto::from).toList(),
                m.version());
    }

    private static TeamSummaryDto summaryOf(TeamId id, Map<TeamId, Team> teams) {
        Team t = teams.get(id);
        return t == null
                ? new TeamSummaryDto(id.value(), "(不明なチーム)", 0)
                : TeamSummaryDto.from(t);
    }
}
