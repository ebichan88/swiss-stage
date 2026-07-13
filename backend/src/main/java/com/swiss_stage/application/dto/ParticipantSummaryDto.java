package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Participant;

/** 対局・順位表で使う参加者の要約 */
public record ParticipantSummaryDto(String id, String name, String organization) {

    public static ParticipantSummaryDto from(Participant p) {
        return new ParticipantSummaryDto(p.id().value(), p.name(), p.organization());
    }
}
