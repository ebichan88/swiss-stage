package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.Standing;

/** 順位表の1行。勝点・SOS・SOSOSは内部の2倍整数を実数に戻して返す(勝=1, 分=0.5) */
public record StandingDto(
        int rank,
        ParticipantSummaryDto participant,
        double wins,
        int losses,
        double sos,
        double sosos,
        boolean hadBye) {

    public static StandingDto from(Standing s, Participant participant) {
        return new StandingDto(
                s.rank(),
                ParticipantSummaryDto.from(participant),
                s.points() / 2.0,
                s.losses(),
                s.sos() / 2.0,
                s.sosos() / 2.0,
                s.hadBye());
    }
}
