package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMember;
import com.swiss_stage.domain.model.TeamMemberId;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;

final class TeamItemMapper {

    private TeamItemMapper() {}

    static TeamItem toItem(TournamentId tournamentId, Team t) {
        var item = new TeamItem();
        item.setPk(DynamoDbKeys.pk(tournamentId));
        item.setSk(DynamoDbKeys.teamSk(t.id()));
        item.setEntityType(TeamItem.ENTITY_TYPE);
        item.setTeamId(t.id().value());
        item.setName(t.name());
        item.setEntryOrder(t.entryOrder());
        item.setStatus(t.status().name());
        item.setGroupId(t.groupId().value());
        item.setMembers(t.members().stream().map(TeamItemMapper::toMemberItem).toList());
        return item;
    }

    static Team toDomain(TeamItem item) {
        List<TeamMember> members = item.getMembers() == null
                ? List.of()
                : item.getMembers().stream().map(TeamItemMapper::toMemberDomain).toList();
        return new Team(
                new TeamId(item.getTeamId()),
                item.getName(),
                item.getEntryOrder(),
                ParticipantStatus.valueOf(item.getStatus()),
                new GroupId(item.getGroupId()),
                members);
    }

    private static TeamMemberItem toMemberItem(TeamMember m) {
        var memberItem = new TeamMemberItem();
        memberItem.setMemberId(m.id().value());
        memberItem.setName(m.name());
        memberItem.setRank(m.rank() == null ? null : m.rank().name());
        memberItem.setBoardPosition(m.boardPosition());
        return memberItem;
    }

    private static TeamMember toMemberDomain(TeamMemberItem item) {
        return new TeamMember(
                new TeamMemberId(item.getMemberId()),
                item.getName(),
                item.getRank() == null ? null : Rank.valueOf(item.getRank()),
                item.getBoardPosition());
    }
}
