package com.swiss_stage.unit.domain;

import static com.swiss_stage.unit.domain.TeamTestData.match;
import static com.swiss_stage.unit.domain.TeamTestData.team;
import static com.swiss_stage.unit.domain.TeamTestData.teams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.service.PairingRelaxation;
import com.swiss_stage.domain.service.TeamSwissPairingService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamSwissPairingServiceTest {

    private final TeamSwissPairingService service = new TeamSwissPairingService();

    @Test
    @DisplayName("初回ラウンドはシードなし・エントリー順のみで隣同士が組まれる")
    void 初回ラウンドはエントリー順のみ() {
        List<Team> ts = teams(8);

        TeamSwissPairingService.PairingResult result = service.pair(ts, List.of(), 1);

        assertThat(result.pairs()).hasSize(4);
        assertThat(result.hasBye()).isFalse();
        assertThat(result.pairs().get(0).firstId()).isEqualTo(ts.get(0).id());
        assertThat(result.pairs().get(0).secondId()).isEqualTo(ts.get(1).id());
        assertThat(result.pairs().get(3).firstId()).isEqualTo(ts.get(6).id());
        assertThat(result.pairs().get(3).secondId()).isEqualTo(ts.get(7).id());
    }

    @Test
    @DisplayName("初回ラウンド(奇数)は末尾のチームにBYEが付く")
    void 初回ラウンド奇数() {
        List<Team> ts = teams(5);

        TeamSwissPairingService.PairingResult result = service.pair(ts, List.of(), 1);

        assertThat(result.pairs()).hasSize(2);
        assertThat(result.byeId()).isEqualTo(ts.get(4).id());
    }

    @Test
    @DisplayName("第2ラウンド: 同じ勝点同士(勝者vs勝者、敗者vs敗者)が組まれる")
    void スコアグループ内ペアリング() {
        List<Team> ts = teams(4);
        List<TeamMatch> round1 = List.of(
                match(1, ts.get(0), ts.get(2),
                        MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN),
                match(1, ts.get(1), ts.get(3),
                        MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN));

        TeamSwissPairingService.PairingResult result = service.pair(ts, round1, 2);

        Set<Set<TeamId>> pairSets = new HashSet<>();
        result.pairs().forEach(p -> pairSets.add(Set.of(p.firstId(), p.secondId())));
        assertThat(pairSets).containsExactlyInAnyOrder(
                Set.of(ts.get(0).id(), ts.get(1).id()),
                Set.of(ts.get(2).id(), ts.get(3).id()));
    }

    @Test
    @DisplayName("再戦は組まれない(全対戦済みの場合のみ緩和され記録される)")
    void 再戦禁止と緩和() {
        List<Team> ts = teams(4);
        MatchResult[] win3 = {MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN};
        List<TeamMatch> played = List.of(
                match(1, ts.get(0), ts.get(1), win3),
                match(1, ts.get(2), ts.get(3), win3),
                match(2, ts.get(0), ts.get(2), win3),
                match(2, ts.get(1), ts.get(3), win3));

        TeamSwissPairingService.PairingResult result = service.pair(ts, played, 3);
        Set<Set<TeamId>> pairSets = new HashSet<>();
        result.pairs().forEach(p -> pairSets.add(Set.of(p.firstId(), p.secondId())));
        assertThat(pairSets).containsExactlyInAnyOrder(
                Set.of(ts.get(0).id(), ts.get(3).id()),
                Set.of(ts.get(1).id(), ts.get(2).id()));
        assertThat(result.relaxations()).isEmpty();

        List<TeamMatch> allPlayed = new ArrayList<>(played);
        allPlayed.add(match(3, ts.get(0), ts.get(3), win3));
        allPlayed.add(match(3, ts.get(1), ts.get(2), win3));
        TeamSwissPairingService.PairingResult round4 = service.pair(ts, allPlayed, 4);
        assertThat(round4.relaxations()).contains(PairingRelaxation.REMATCH);
    }

    @Test
    @DisplayName("BYEはBYE未経験かつ最下位グループのチームに付き、同一チームに2回付かない")
    void BYE重複禁止() {
        List<Team> ts = teams(3);
        List<TeamMatch> played = List.of(
                match(1, ts.get(0), ts.get(1),
                        MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN),
                TeamTestData.bye(1, ts.get(2)));

        TeamSwissPairingService.PairingResult result = service.pair(ts, played, 2);

        assertThat(result.byeId()).isEqualTo(ts.get(1).id());
        assertThat(result.relaxations()).isEmpty();
    }

    @Test
    @DisplayName("棄権チーム(WITHDRAWN)はマッチング対象から除外される")
    void 棄権チームの除外() {
        List<Team> ts = new ArrayList<>(teams(4));
        Team withdrawn = ts.get(3).withdraw();
        ts.set(3, withdrawn);
        MatchResult[] win3 = {MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN};
        List<TeamMatch> round1 = List.of(
                match(1, ts.get(0), ts.get(1), win3),
                TeamTestData.bye(1, ts.get(2)));

        TeamSwissPairingService.PairingResult result = service.pair(ts, round1, 2);

        assertThat(result.pairs()).hasSize(1);
        result.pairs().forEach(pair -> {
            assertThat(pair.firstId()).isNotEqualTo(withdrawn.id());
            assertThat(pair.secondId()).isNotEqualTo(withdrawn.id());
        });
    }

    @Test
    @DisplayName("チームが2つ未満の場合は例外になる")
    void チーム数不足() {
        assertThatThrownBy(() -> service.pair(List.of(team(1)), List.of(), 1))
                .isInstanceOf(DomainException.class);
    }
}
