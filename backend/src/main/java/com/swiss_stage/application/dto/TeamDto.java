package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Team;
import java.util.List;

public record TeamDto(
        String id,
        String name,
        int entryOrder,
        ParticipantStatus status,
        String groupId,
        List<TeamMemberDto> members) {

    public static TeamDto from(Team t) {
        return new TeamDto(
                t.id().value(), t.name(), t.entryOrder(), t.status(), t.groupId().value(),
                t.members().stream().map(TeamMemberDto::from).toList());
    }
}
