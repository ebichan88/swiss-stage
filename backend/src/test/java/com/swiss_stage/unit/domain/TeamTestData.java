package com.swiss_stage.unit.domain;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import java.util.ArrayList;
import java.util.List;

/** 団体戦ドメインテスト用のデータビルダー(実在のチーム名は使わない) */
final class TeamTestData {

    private TeamTestData() {}

    static final GroupId GROUP_ID = new GroupId("01TESTGROUP000000000000000");

    static Team team(int entryOrder) {
        return new Team(
                new TeamId("T" + String.format("%03d", entryOrder)),
                "チーム" + entryOrder,
                entryOrder,
                ParticipantStatus.ACTIVE,
                GROUP_ID,
                List.of());
    }

    static List<Team> teams(int count) {
        List<Team> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(team(i));
        }
        return list;
    }

    /** teamSize=3の全ボード決着済み対局。resultsは team1の各ボードの結果(長さ3) */
    static TeamMatch match(int round, Team t1, Team t2, MatchResult... boardResults) {
        return TeamMatch.pairOf(round, 1, t1.id(), t2.id(), boardResults.length, GROUP_ID)
                .withBoardResults(List.of(boardResults));
    }

    static TeamMatch bye(int round, Team t) {
        return TeamMatch.byeOf(round, 99, t.id(), GROUP_ID);
    }
}
