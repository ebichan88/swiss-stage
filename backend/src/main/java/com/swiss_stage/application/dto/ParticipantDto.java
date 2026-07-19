package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Rank;

public record ParticipantDto(
        String id,
        String name,
        String organization,
        Rank rank,
        int seedOrder,
        ParticipantStatus status,
        String groupId) {

    public static ParticipantDto from(Participant p) {
        return new ParticipantDto(
                p.id().value(), p.name(), p.organization(), p.rank(), p.seedOrder(), p.status(),
                p.groupId().value());
    }
}
