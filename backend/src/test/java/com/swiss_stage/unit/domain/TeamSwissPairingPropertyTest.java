package com.swiss_stage.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.service.TeamSwissPairingService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

/**
 * 団体戦スイス方式マッチングのプロパティテスト。
 * 16〜60チーム・5回戦をシミュレートし、絶対制約(再戦なし・BYE重複なし・全員毎ラウンド1回登場)を
 * 機械的に検証する(05_swiss_pairing_algorithm.md §5.2)。
 */
class TeamSwissPairingPropertyTest {

    private static final int ROUNDS = 5;
    private static final int TEAM_SIZE = 3;

    private final TeamSwissPairingService service = new TeamSwissPairingService();

    @Property(tries = 30)
    void 全ラウンドを通して絶対制約が守られる(
            @ForAll @IntRange(min = 16, max = 60) int teamCount,
            @ForAll long resultSeed) {
        List<Team> teams = TeamTestData.teams(teamCount);
        Random random = new Random(resultSeed);
        List<TeamMatch> allMatches = new ArrayList<>();
        Set<Set<String>> playedPairs = new HashSet<>();
        Set<TeamId> byeReceivers = new HashSet<>();

        for (int round = 1; round <= ROUNDS; round++) {
            TeamSwissPairingService.PairingResult result = service.pair(teams, allMatches, round);

            Set<TeamId> appeared = new HashSet<>();
            result.pairs().forEach(pair -> {
                assertThat(appeared.add(pair.firstId())).isTrue();
                assertThat(appeared.add(pair.secondId())).isTrue();
            });
            if (result.hasBye()) {
                assertThat(appeared.add(result.byeId())).isTrue();
            }
            assertThat(appeared).hasSize(teamCount);

            if (!result.relaxations().contains(
                    com.swiss_stage.domain.service.PairingRelaxation.REMATCH)) {
                for (TeamSwissPairingService.PairingResult.Pair pair : result.pairs()) {
                    Set<String> key = Set.of(pair.firstId().value(), pair.secondId().value());
                    assertThat(playedPairs).doesNotContain(key);
                }
            }
            result.pairs().forEach(pair ->
                    playedPairs.add(Set.of(pair.firstId().value(), pair.secondId().value())));

            if (result.hasBye()) {
                if (!result.relaxations().contains(
                        com.swiss_stage.domain.service.PairingRelaxation.BYE_REPEAT)) {
                    assertThat(byeReceivers).doesNotContain(result.byeId());
                }
                byeReceivers.add(result.byeId());
            }

            // 4チーム以上・5回戦では絶対制約の緩和は発生しないはず
            assertThat(result.relaxations()).isEmpty();

            int table = 1;
            for (TeamSwissPairingService.PairingResult.Pair pair : result.pairs()) {
                MatchResult boardResult = random.nextBoolean()
                        ? MatchResult.PLAYER1_WIN
                        : MatchResult.PLAYER2_WIN;
                allMatches.add(TeamMatch.pairOf(
                                round, table++, pair.firstId(), pair.secondId(), TEAM_SIZE,
                                TeamTestData.GROUP_ID)
                        .withBoardResults(List.of(boardResult, boardResult, boardResult)));
            }
            if (result.hasBye()) {
                allMatches.add(TeamMatch.byeOf(round, table, result.byeId(), TeamTestData.GROUP_ID));
            }
        }
    }
}
