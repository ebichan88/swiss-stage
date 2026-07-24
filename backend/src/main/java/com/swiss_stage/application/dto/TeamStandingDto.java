package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamStanding;

/** 団体戦順位表の1行。勝点・SOS・SOSOSは内部の2倍整数を実数に戻して返す(勝=1, 分=0.5) */
public record TeamStandingDto(
        int rank,
        TeamSummaryDto team,
        double wins,
        int losses,
        double sos,
        double sosos,
        boolean hadBye) {

    public static TeamStandingDto from(TeamStanding s, Team team) {
        return new TeamStandingDto(
                s.rank(),
                TeamSummaryDto.from(team),
                s.points() / 2.0,
                s.losses(),
                s.sos() / 2.0,
                s.sosos() / 2.0,
                s.hadBye());
    }
}
