package com.swiss_stage.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.service.PairingOptions;
import com.swiss_stage.domain.service.PairingResult;
import com.swiss_stage.domain.service.SwissPairingService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

/**
 * スイス方式マッチングのプロパティテスト。
 * 16〜300名・5回戦をシミュレートし、絶対制約(再戦なし・BYE重複なし・全員毎ラウンド1回登場)を
 * 機械的に検証する(05_swiss_pairing_algorithm.md §4-7)。
 */
class SwissPairingPropertyTest {

    private static final int ROUNDS = 5;

    private final SwissPairingService service = new SwissPairingService();

    @Property(tries = 30)
    void 全ラウンドを通して絶対制約が守られる(
            @ForAll @IntRange(min = 16, max = 300) int participantCount,
            @ForAll long resultSeed) {
        List<Participant> participants = TestData.participants(participantCount);
        Random random = new Random(resultSeed);
        List<Match> allMatches = new ArrayList<>();
        Set<Set<String>> playedPairs = new HashSet<>();
        Set<ParticipantId> byeReceivers = new HashSet<>();

        for (int round = 1; round <= ROUNDS; round++) {
            PairingResult result = service.pair(participants, allMatches, round, PairingOptions.defaults());

            // 全員がちょうど1回登場する
            Set<ParticipantId> appeared = new HashSet<>();
            result.pairs().forEach(pair -> {
                assertThat(appeared.add(pair.player1Id())).isTrue();
                assertThat(appeared.add(pair.player2Id())).isTrue();
            });
            if (result.hasBye()) {
                assertThat(appeared.add(result.byeParticipantId())).isTrue();
            }
            assertThat(appeared).hasSize(participantCount);

            // 再戦禁止(緩和が報告されていない限り)
            if (!result.relaxations().contains(
                    com.swiss_stage.domain.service.PairingRelaxation.REMATCH)) {
                for (PairingResult.Pair pair : result.pairs()) {
                    Set<String> key = Set.of(pair.player1Id().value(), pair.player2Id().value());
                    assertThat(playedPairs).doesNotContain(key);
                }
            }
            result.pairs().forEach(pair ->
                    playedPairs.add(Set.of(pair.player1Id().value(), pair.player2Id().value())));

            // BYE重複禁止(緩和が報告されていない限り)
            if (result.hasBye()) {
                if (!result.relaxations().contains(
                        com.swiss_stage.domain.service.PairingRelaxation.BYE_REPEAT)) {
                    assertThat(byeReceivers).doesNotContain(result.byeParticipantId());
                }
                byeReceivers.add(result.byeParticipantId());
            }

            // 16名以上・5回戦では絶対制約の緩和は発生しないはず
            assertThat(result.relaxations()).isEmpty();

            // ランダムな結果を入力して次ラウンドへ
            int table = 1;
            for (PairingResult.Pair pair : result.pairs()) {
                MatchResult matchResult = random.nextBoolean()
                        ? MatchResult.PLAYER1_WIN
                        : MatchResult.PLAYER2_WIN;
                allMatches.add(Match.pairOf(round, table++, pair.player1Id(), pair.player2Id())
                        .withResult(matchResult));
            }
            if (result.hasBye()) {
                allMatches.add(Match.byeOf(round, table, result.byeParticipantId()));
            }
        }
    }
}
