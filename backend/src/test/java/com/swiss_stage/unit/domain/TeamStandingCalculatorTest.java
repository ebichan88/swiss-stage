package com.swiss_stage.unit.domain;

import static com.swiss_stage.unit.domain.TeamTestData.bye;
import static com.swiss_stage.unit.domain.TeamTestData.match;
import static com.swiss_stage.unit.domain.TeamTestData.teams;
import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamStanding;
import com.swiss_stage.domain.service.TeamStandingCalculator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamStandingCalculatorTest {

    private final TeamStandingCalculator calculator = new TeamStandingCalculator();

    private Map<TeamId, TeamStanding> byId(List<TeamStanding> standings) {
        return standings.stream().collect(Collectors.toMap(TeamStanding::teamId, Function.identity()));
    }

    @Test
    @DisplayName("ボード点数の合計を比較して勝ち=2点・負け=0点・BYE=2点(2倍整数)が集計される")
    void 勝点の集計() {
        List<Team> ts = teams(5);
        List<TeamMatch> matches = List.of(
                match(1, ts.get(0), ts.get(1),
                        MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER2_WIN),
                bye(1, ts.get(4)));

        var standings = byId(calculator.calculate(ts, matches));

        assertThat(standings.get(ts.get(0).id()).points()).isEqualTo(2); // 2-1で勝ち
        assertThat(standings.get(ts.get(1).id()).points()).isZero();
        assertThat(standings.get(ts.get(4).id()).points()).isEqualTo(2);
        assertThat(standings.get(ts.get(4).id()).hadBye()).isTrue();
        assertThat(standings.get(ts.get(0).id()).wins()).isEqualTo(1);
        assertThat(standings.get(ts.get(1).id()).losses()).isEqualTo(1);
    }

    @Test
    @DisplayName("ボード点数の合計が同点なら引き分け(1点)になる")
    void ボード内訳が同点なら引き分け() {
        List<Team> ts = teams(2);
        List<TeamMatch> matches = List.of(
                match(1, ts.get(0), ts.get(1),
                        MatchResult.PLAYER1_WIN, MatchResult.PLAYER2_WIN, MatchResult.DRAW));

        var standings = byId(calculator.calculate(ts, matches));

        assertThat(standings.get(ts.get(0).id()).points()).isEqualTo(1);
        assertThat(standings.get(ts.get(1).id()).points()).isEqualTo(1);
        assertThat(standings.get(ts.get(0).id()).draws()).isEqualTo(1);
        assertThat(standings.get(ts.get(1).id()).draws()).isEqualTo(1);
    }

    @Test
    @DisplayName("一部ボードのみ決着している対局は集計に含まれない")
    void 部分決着は集計に含まれない() {
        List<Team> ts = teams(2);
        TeamMatch partial = TeamMatch.pairOf(1, 1, ts.get(0).id(), ts.get(1).id(), 3, TeamTestData.GROUP_ID)
                .withBoardResults(List.of(MatchResult.PLAYER1_WIN, MatchResult.NONE, MatchResult.NONE));

        var standings = byId(calculator.calculate(ts, List.of(partial)));

        assertThat(standings.get(ts.get(0).id()).points()).isZero();
        assertThat(standings.get(ts.get(1).id()).points()).isZero();
    }

    @Test
    @DisplayName("SOSは対戦相手の勝点合計。BYEラウンドは相手なしとして除外される")
    void SOSはBYEを除外する() {
        List<Team> ts = teams(4);
        MatchResult[] win = {MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN};
        List<TeamMatch> matches = List.of(
                match(1, ts.get(0), ts.get(1), win),
                match(1, ts.get(2), ts.get(3), win),
                bye(2, ts.get(0)),
                match(2, ts.get(1), ts.get(2), win));

        var standings = byId(calculator.calculate(ts, matches));

        // ts(1)の最終勝点=2(R2でts(2)に勝ち)。ts(0)のSOSはts(1)の2のみ(BYE除外)
        assertThat(standings.get(ts.get(0).id()).sos()).isEqualTo(2);
    }

    @Test
    @DisplayName("順位は勝点→SOS→SOSOSの順で決まり、同点は同順位(1,2,2,4形式)になる")
    void 同順位の付与() {
        List<Team> ts = teams(4);
        MatchResult[] win = {MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN};
        List<TeamMatch> matches = List.of(
                match(1, ts.get(0), ts.get(1), win),
                match(1, ts.get(2), ts.get(3), win),
                match(2, ts.get(0), ts.get(2), win),
                match(2, ts.get(1), ts.get(3), win));

        List<TeamStanding> standings = calculator.calculate(ts, matches);

        assertThat(standings.get(0).teamId()).isEqualTo(ts.get(0).id());
        assertThat(standings.get(0).rank()).isEqualTo(1);
        assertThat(standings.get(1).rank()).isEqualTo(2);
        assertThat(standings.get(2).rank()).isEqualTo(2);
        assertThat(standings.get(3).rank()).isEqualTo(4);
    }

    @Test
    @DisplayName("対局がない場合は全チーム0点で同順位1位になる")
    void 対局なし() {
        List<Team> ts = teams(3);

        List<TeamStanding> standings = calculator.calculate(ts, List.of());

        assertThat(standings).allSatisfy(s -> {
            assertThat(s.points()).isZero();
            assertThat(s.rank()).isEqualTo(1);
        });
    }
}
