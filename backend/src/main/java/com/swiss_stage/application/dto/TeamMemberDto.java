package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.TeamMember;

public record TeamMemberDto(String id, String name, Rank rank, Integer boardPosition) {

    public static TeamMemberDto from(TeamMember m) {
        return new TeamMemberDto(m.id().value(), m.name(), m.rank(), m.boardPosition());
    }
}
