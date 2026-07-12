package com.swiss_stage.unit.domain;

import static com.swiss_stage.unit.domain.TestData.match;
import static com.swiss_stage.unit.domain.TestData.participant;
import static com.swiss_stage.unit.domain.TestData.participants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.service.PairingOptions;
import com.swiss_stage.domain.service.PairingRelaxation;
import com.swiss_stage.domain.service.PairingResult;
import com.swiss_stage.domain.service.SwissPairingService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SwissPairingServiceTest {

    private final SwissPairingService service = new SwissPairingService();
    private final PairingOptions defaults = PairingOptions.defaults();

    @Test
    @DisplayName("初回ラウンド(偶数): 棋力未入力ならシード順で隣同士が組まれる")
    void 初回ラウンド偶数() {
        List<Participant> ps = participants(8);

        PairingResult result = service.pair(ps, List.of(), 1, defaults);

        assertThat(result.pairs()).hasSize(4);
        assertThat(result.hasBye()).isFalse();
        // シード1位 vs 2位、3位 vs 4位 …
        assertThat(result.pairs().get(0).player1Id()).isEqualTo(ps.get(0).id());
        assertThat(result.pairs().get(0).player2Id()).isEqualTo(ps.get(1).id());
        assertThat(result.pairs().get(3).player1Id()).isEqualTo(ps.get(6).id());
        assertThat(result.pairs().get(3).player2Id()).isEqualTo(ps.get(7).id());
    }

    @Test
    @DisplayName("初回ラウンド: 棋力の強い順にソートされ、棋力の近い者同士が組まれる")
    void 初回ラウンド棋力順() {
        // シード順と棋力順をずらして、棋力が優先されることを確認する
        Participant kyu5 = participant(1, Rank.KYU_5);
        Participant dan3 = participant(2, Rank.DAN_3);
        Participant kyu1 = participant(3, Rank.KYU_1);
        Participant dan1 = participant(4, Rank.DAN_1);
        List<Participant> ps = List.of(kyu5, dan3, kyu1, dan1);

        PairingResult result = service.pair(ps, List.of(), 1, defaults);

        // 強い順: 三段 > 初段 > 1級 > 5級 → (三段, 初段), (1級, 5級)
        assertThat(result.pairs().get(0).player1Id()).isEqualTo(dan3.id());
        assertThat(result.pairs().get(0).player2Id()).isEqualTo(dan1.id());
        assertThat(result.pairs().get(1).player1Id()).isEqualTo(kyu1.id());
        assertThat(result.pairs().get(1).player2Id()).isEqualTo(kyu5.id());
    }

    @Test
    @DisplayName("初回ラウンド(奇数): 棋力未入力者は末尾に置かれ、最弱者にBYEが付く")
    void 初回ラウンド奇数() {
        Participant dan1 = participant(1, Rank.DAN_1);
        Participant kyu20 = participant(2, Rank.KYU_20);
        Participant kyu1 = participant(3, Rank.KYU_1);
        Participant dan9 = participant(4, Rank.DAN_9);
        Participant unrated = participant(5); // 棋力未入力は20級より弱い扱い
        List<Participant> ps = List.of(dan1, kyu20, kyu1, dan9, unrated);

        PairingResult result = service.pair(ps, List.of(), 1, defaults);

        // 強い順: 九段 > 初段 > 1級 > 20級 > 未入力 → BYEは未入力者
        assertThat(result.pairs()).hasSize(2);
        assertThat(result.byeParticipantId()).isEqualTo(unrated.id());
        assertThat(result.pairs().get(0).player1Id()).isEqualTo(dan9.id());
        assertThat(result.pairs().get(0).player2Id()).isEqualTo(dan1.id());
        assertThat(result.pairs().get(1).player1Id()).isEqualTo(kyu1.id());
        assertThat(result.pairs().get(1).player2Id()).isEqualTo(kyu20.id());
    }

    @Test
    @DisplayName("初回ラウンドのランダムオプションは同一シードで再現できる")
    void 初回ラウンドランダム() {
        List<Participant> ps = participants(8);
        PairingOptions random = new PairingOptions(true, true, 42L);

        PairingResult first = service.pair(ps, List.of(), 1, random);
        PairingResult second = service.pair(ps, List.of(), 1, random);

        assertThat(first.pairs()).isEqualTo(second.pairs());
    }

    @Test
    @DisplayName("第2ラウンド: 同じ勝点同士(勝者vs勝者、敗者vs敗者)が組まれる")
    void スコアグループ内ペアリング() {
        List<Participant> ps = participants(4);
        List<Match> round1 = List.of(
                match(1, ps.get(0), ps.get(2), MatchResult.PLAYER1_WIN),
                match(1, ps.get(1), ps.get(3), MatchResult.PLAYER1_WIN));

        PairingResult result = service.pair(ps, round1, 2, defaults);

        Set<Set<ParticipantId>> pairSets = new HashSet<>();
        result.pairs().forEach(p -> pairSets.add(Set.of(p.player1Id(), p.player2Id())));
        assertThat(pairSets).containsExactlyInAnyOrder(
                Set.of(ps.get(0).id(), ps.get(1).id()), // 勝者同士
                Set.of(ps.get(2).id(), ps.get(3).id())); // 敗者同士
    }

    @Test
    @DisplayName("再戦は組まれない(スコアグループ内に候補が再戦相手しかいなくてもフロートする)")
    void 再戦禁止() {
        List<Participant> ps = participants(4);
        // R1: P1-P2, P3-P4 / R2: P1-P3, P2-P4 → R3はP1-P4, P2-P3しか組めない
        List<Match> played = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN),
                match(1, ps.get(2), ps.get(3), MatchResult.PLAYER1_WIN),
                match(2, ps.get(0), ps.get(2), MatchResult.PLAYER1_WIN),
                match(2, ps.get(1), ps.get(3), MatchResult.PLAYER1_WIN));

        PairingResult result = service.pair(ps, played, 3, defaults);

        Set<Set<ParticipantId>> pairSets = new HashSet<>();
        result.pairs().forEach(p -> pairSets.add(Set.of(p.player1Id(), p.player2Id())));
        assertThat(pairSets).containsExactlyInAnyOrder(
                Set.of(ps.get(0).id(), ps.get(3).id()),
                Set.of(ps.get(1).id(), ps.get(2).id()));
        assertThat(result.relaxations()).isEmpty();
    }

    @Test
    @DisplayName("全員と対戦済みの場合のみ再戦が緩和され、結果に記録される")
    void 再戦の緩和() {
        List<Participant> ps = participants(4);
        // 4名で総当たり済み → 第4ラウンドは再戦せざるを得ない
        List<Match> played = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN),
                match(1, ps.get(2), ps.get(3), MatchResult.PLAYER1_WIN),
                match(2, ps.get(0), ps.get(2), MatchResult.PLAYER1_WIN),
                match(2, ps.get(1), ps.get(3), MatchResult.PLAYER1_WIN),
                match(3, ps.get(0), ps.get(3), MatchResult.PLAYER1_WIN),
                match(3, ps.get(1), ps.get(2), MatchResult.PLAYER1_WIN));

        PairingResult result = service.pair(ps, played, 4, defaults);

        assertThat(result.pairs()).hasSize(2);
        assertThat(result.relaxations()).contains(PairingRelaxation.REMATCH);
    }

    @Test
    @DisplayName("BYEはBYE未経験かつ最下位グループの参加者に付き、同一参加者に2回付かない")
    void BYE重複禁止() {
        List<Participant> ps = participants(3);
        // R1: P3がBYE、P1がP2に勝ち
        List<Match> played = new ArrayList<>();
        played.add(match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN));
        played.add(TestData.bye(1, ps.get(2)));

        PairingResult result = service.pair(ps, played, 2, defaults);

        // BYE経験のないP2(最下位0点)がBYEになる
        assertThat(result.byeParticipantId()).isEqualTo(ps.get(1).id());
        assertThat(result.relaxations()).isEmpty();
    }

    @Test
    @DisplayName("同一所属同士の対戦は可能な限り避けられる")
    void 同一所属回避() {
        List<Participant> ps = List.of(
                participant(1, "A社"),
                participant(2, "A社"),
                participant(3, "B社"),
                participant(4, "B社"));
        // R1で全員1勝0敗/0勝1敗に分かれても、A社同士・B社同士は組まれないこと
        List<Match> round1 = List.of(
                match(1, ps.get(0), ps.get(2), MatchResult.PLAYER1_WIN),  // A社P1勝ち
                match(1, ps.get(1), ps.get(3), MatchResult.PLAYER1_WIN)); // A社P2勝ち

        PairingResult result = service.pair(ps, round1, 2, defaults);

        for (PairingResult.Pair pair : result.pairs()) {
            Participant p1 = ps.stream().filter(p -> p.id().equals(pair.player1Id())).findFirst().orElseThrow();
            Participant p2 = ps.stream().filter(p -> p.id().equals(pair.player2Id())).findFirst().orElseThrow();
            assertThat(p1.hasSameOrganization(p2)).isFalse();
        }
        assertThat(result.relaxations()).isEmpty();
    }

    @Test
    @DisplayName("全員同一所属の場合は所属回避が緩和され、結果に記録される")
    void 同一所属回避の緩和() {
        List<Participant> ps = List.of(
                participant(1, "A社"),
                participant(2, "A社"),
                participant(3, "A社"),
                participant(4, "A社"));
        List<Match> round1 = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN),
                match(1, ps.get(2), ps.get(3), MatchResult.PLAYER1_WIN));

        PairingResult result = service.pair(ps, round1, 2, defaults);

        assertThat(result.pairs()).hasSize(2);
        assertThat(result.relaxations()).contains(PairingRelaxation.SAME_ORGANIZATION);
    }

    @Test
    @DisplayName("棄権者(WITHDRAWN)はマッチング対象から除外される")
    void 棄権者の除外() {
        List<Participant> ps = new ArrayList<>(participants(4));
        Participant withdrawn = ps.get(3).withdraw();
        ps.set(3, withdrawn);
        List<Match> round1 = List.of(
                match(1, ps.get(0), ps.get(1), MatchResult.PLAYER1_WIN),
                match(1, ps.get(2), withdrawn, MatchResult.PLAYER1_WIN));

        PairingResult result = service.pair(ps, round1, 2, defaults);

        // 残り3名: 1ペア + BYE。棄権者はどこにも現れない
        assertThat(result.pairs()).hasSize(1);
        assertThat(result.hasBye()).isTrue();
        result.pairs().forEach(pair -> {
            assertThat(pair.player1Id()).isNotEqualTo(withdrawn.id());
            assertThat(pair.player2Id()).isNotEqualTo(withdrawn.id());
        });
        assertThat(result.byeParticipantId()).isNotEqualTo(withdrawn.id());
    }

    @Test
    @DisplayName("参加者が2名未満の場合は例外になる")
    void 参加者不足() {
        assertThatThrownBy(() -> service.pair(participants(1), List.of(), 1, defaults))
                .isInstanceOf(DomainException.class);
    }
}
