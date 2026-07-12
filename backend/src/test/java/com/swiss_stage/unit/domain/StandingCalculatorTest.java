package com.swiss_stage.unit.domain;

import static com.swiss_stage.unit.domain.TestData.bye;
import static com.swiss_stage.unit.domain.TestData.match;
import static com.swiss_stage.unit.domain.TestData.participants;
import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Standing;
import com.swiss_stage.domain.service.StandingCalculator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StandingCalculatorTest {

    private final StandingCalculator calculator = new StandingCalculator();

    private Map<ParticipantId, Standing> byId(List<Standing> standings) {
        return standings.stream().collect(Collectors.toMap(Standing::participantId, Function.identity()));
    }

    @Test
    @DisplayName("勝ち=2点、引き分け=1点、負け=0点、BYE=2点(2倍整数)が集計される")
    void 勝点の集計() {
        List<Participant> ps = participants(5);
        List<Match> matches = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN),
                match(1, ps.get(2), ps.get(3), MatchResult.DRAW),
                bye(1, ps.get(4)));

        var standings = byId(calculator.calculate(ps, matches));

        assertThat(standings.get(ps.get(0).id()).points()).isEqualTo(2);
        assertThat(standings.get(ps.get(1).id()).points()).isZero();
        assertThat(standings.get(ps.get(2).id()).points()).isEqualTo(1);
        assertThat(standings.get(ps.get(3).id()).points()).isEqualTo(1);
        assertThat(standings.get(ps.get(4).id()).points()).isEqualTo(2);
        assertThat(standings.get(ps.get(4).id()).hadBye()).isTrue();
        assertThat(standings.get(ps.get(0).id()).wins()).isEqualTo(1);
        assertThat(standings.get(ps.get(1).id()).losses()).isEqualTo(1);
        assertThat(standings.get(ps.get(2).id()).draws()).isEqualTo(1);
        assertThat(standings.get(ps.get(4).id()).wins()).isEqualTo(1); // BYEは1勝扱い
    }

    @Test
    @DisplayName("両者負け(BOTH_LOSE)は両者0点・両者1敗になる")
    void 両者負け() {
        List<Participant> ps = participants(2);
        List<Match> matches = List.of(match(1, ps.get(0), ps.get(1), MatchResult.BOTH_LOSE));

        var standings = byId(calculator.calculate(ps, matches));

        assertThat(standings.get(ps.get(0).id()).points()).isZero();
        assertThat(standings.get(ps.get(1).id()).points()).isZero();
        assertThat(standings.get(ps.get(0).id()).losses()).isEqualTo(1);
        assertThat(standings.get(ps.get(1).id()).losses()).isEqualTo(1);
    }

    @Test
    @DisplayName("未入力(NONE)の対局は集計に含まれない")
    void 未入力は無視() {
        List<Participant> ps = participants(2);
        List<Match> matches = List.of(
                Match.pairOf(1, 1, ps.get(0).id(), ps.get(1).id())); // result = NONE

        var standings = byId(calculator.calculate(ps, matches));

        assertThat(standings.get(ps.get(0).id()).points()).isZero();
        assertThat(standings.get(ps.get(0).id()).sos()).isZero();
    }

    @Test
    @DisplayName("SOSは対戦相手の勝点合計。BYEラウンドは相手なしとして除外される")
    void SOSはBYEを除外する() {
        List<Participant> ps = participants(4);
        // P1: R1でP2に勝ち、R2でBYE → SOSはP2の勝点のみ
        List<Match> matches = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN),
                match(1, ps.get(2), ps.get(3), MatchResult.PLAYER1_WIN),
                bye(2, ps.get(0)),
                match(2, ps.get(1), ps.get(2), MatchResult.PLAYER1_WIN));

        var standings = byId(calculator.calculate(ps, matches));

        // P2の最終勝点 = 2(R2でP3に勝ち)。P1のSOSはP2の2のみ(BYE除外)
        assertThat(standings.get(ps.get(0).id()).sos()).isEqualTo(2);
    }

    @Test
    @DisplayName("SOSOSは対戦相手のSOSの合計になる")
    void SOSOSの計算() {
        List<Participant> ps = participants(4);
        List<Match> matches = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN),
                match(1, ps.get(2), ps.get(3), MatchResult.PLAYER1_WIN),
                match(2, ps.get(0), ps.get(2), MatchResult.PLAYER1_WIN),
                match(2, ps.get(1), ps.get(3), MatchResult.PLAYER1_WIN));

        var standings = byId(calculator.calculate(ps, matches));

        // P1の対戦相手はP2(SOS=P1+P4の勝点=4+0=4... P2の相手はP1,P4)とP3
        Standing p2 = standings.get(ps.get(1).id());
        Standing p3 = standings.get(ps.get(2).id());
        Standing p1 = standings.get(ps.get(0).id());
        assertThat(p1.sosos()).isEqualTo(p2.sos() + p3.sos());
    }

    @Test
    @DisplayName("順位は勝点→SOS→SOSOSの順で決まり、同点は同順位(1,2,2,4形式)になる")
    void 同順位の付与() {
        List<Participant> ps = participants(4);
        // P1: 2勝 / P2,P3: 1勝(同SOS/SOSOSになるよう対称に) / P4: 0勝
        List<Match> matches = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN),
                match(1, ps.get(2), ps.get(3), MatchResult.PLAYER1_WIN),
                match(2, ps.get(0), ps.get(2), MatchResult.PLAYER1_WIN),
                match(2, ps.get(1), ps.get(3), MatchResult.PLAYER1_WIN));

        List<Standing> standings = calculator.calculate(ps, matches);

        assertThat(standings.get(0).participantId()).isEqualTo(ps.get(0).id());
        assertThat(standings.get(0).rank()).isEqualTo(1);
        // P2(SOS: P1=4,P4=0 → 4) と P3(SOS: P4=0,P1=4 → 4)は完全同キー
        assertThat(standings.get(1).rank()).isEqualTo(2);
        assertThat(standings.get(2).rank()).isEqualTo(2);
        assertThat(standings.get(3).rank()).isEqualTo(4);
    }

    @Test
    @DisplayName("勝点・SOS・SOSOSが同じ2名は直接対決の勝者が上位になる")
    void 直接対決タイブレーク() {
        List<Participant> ps = participants(4);
        // 4名総当たり3回戦。P1とP2は2勝1敗・SOS・SOSOSが完全同値で、直接対決はP1勝ち
        List<Match> matches = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN), // P1がP2に勝ち
                match(1, ps.get(2), ps.get(3), MatchResult.PLAYER1_WIN),
                match(2, ps.get(0), ps.get(2), MatchResult.PLAYER1_WIN),
                match(2, ps.get(1), ps.get(3), MatchResult.PLAYER1_WIN),
                match(3, ps.get(0), ps.get(3), MatchResult.PLAYER2_WIN), // P4がP1に勝ち
                match(3, ps.get(1), ps.get(2), MatchResult.PLAYER1_WIN)); // P2がP3に勝ち

        List<Standing> standings = calculator.calculate(ps, matches);
        var map = byId(standings);
        Standing p1 = map.get(ps.get(0).id());
        Standing p2 = map.get(ps.get(1).id());

        // 前提確認: P1とP2は勝点・SOS・SOSOSが同値
        assertThat(p1.points()).isEqualTo(p2.points());
        assertThat(p1.sos()).isEqualTo(p2.sos());
        assertThat(p1.sosos()).isEqualTo(p2.sosos());
        // 直接対決でP1が勝っているため、P1が上位(別順位)
        assertThat(p1.rank()).isEqualTo(1);
        assertThat(p2.rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("対局がない場合は全員0点で同順位1位になる")
    void 対局なし() {
        List<Participant> ps = participants(3);

        List<Standing> standings = calculator.calculate(ps, List.of());

        assertThat(standings).allSatisfy(s -> {
            assertThat(s.points()).isZero();
            assertThat(s.rank()).isEqualTo(1);
        });
    }
}
