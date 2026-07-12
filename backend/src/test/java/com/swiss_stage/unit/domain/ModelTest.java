package com.swiss_stage.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.RoundStatus;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.model.Visibility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModelTest {

    @Nested
    class TournamentTest {

        private final Tournament preparing = Tournament.create("テスト大会", GameType.GO, 5, "owner-sub");

        @Test
        @DisplayName("PREPARING → IN_PROGRESS → FINISHED と遷移できる")
        void 正常な状態遷移() {
            Tournament started = preparing.start();
            assertThat(started.status()).isEqualTo(TournamentStatus.IN_PROGRESS);

            Tournament advanced = started.advanceRound();
            assertThat(advanced.currentRound()).isEqualTo(1);

            Tournament finished = advanced.finish();
            assertThat(finished.status()).isEqualTo(TournamentStatus.FINISHED);
        }

        @Test
        @DisplayName("開始済みの大会は再度開始できない")
        void 二重開始の禁止() {
            Tournament started = preparing.start();
            assertThatThrownBy(started::start).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("準備中の大会はラウンドを進められない・終了できない")
        void 準備中の操作制限() {
            assertThatThrownBy(preparing::advanceRound).isInstanceOf(DomainException.class);
            assertThatThrownBy(preparing::finish).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("最終ラウンドを超えて進められない")
        void 最終ラウンド超過() {
            Tournament t = Tournament.create("大会", GameType.SHOGI, 1, "owner").start().advanceRound();
            assertThatThrownBy(t::advanceRound).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("所有者判定と公開範囲変更ができる")
        void 所有者と公開範囲() {
            assertThat(preparing.isOwnedBy("owner-sub")).isTrue();
            assertThat(preparing.isOwnedBy("other")).isFalse();
            assertThat(preparing.withVisibility(Visibility.TOKEN).visibility())
                    .isEqualTo(Visibility.TOKEN);
        }

        @Test
        @DisplayName("大会名が空・ラウンド数0は作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> Tournament.create(" ", GameType.GO, 5, "owner"))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Tournament.create("大会", GameType.GO, 0, "owner"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class RoundTest {

        @Test
        @DisplayName("PAIRING → PLAYING → CONFIRMED と遷移できる")
        void 正常な状態遷移() {
            Round round = Round.pairing(1);
            assertThat(round.status()).isEqualTo(RoundStatus.PAIRING);
            Round playing = round.startPlaying();
            Round confirmed = playing.confirm();
            assertThat(confirmed.isConfirmed()).isTrue();
        }

        @Test
        @DisplayName("組み合わせ中のラウンドは確定できない")
        void 不正な遷移() {
            assertThatThrownBy(() -> Round.pairing(1).confirm()).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Round.pairing(1).startPlaying().startPlaying())
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class MatchTest {

        private final ParticipantId p1 = ParticipantId.generate();
        private final ParticipantId p2 = ParticipantId.generate();

        @Test
        @DisplayName("結果の入力・上書きができ、BYEへの変更はできない")
        void 結果入力() {
            Match match = Match.pairOf(1, 1, p1, p2);
            Match decided = match.withResult(MatchResult.PLAYER1_WIN);
            assertThat(decided.pointsFor(p1)).isEqualTo(2);
            assertThat(decided.pointsFor(p2)).isZero();
            assertThatThrownBy(() -> match.withResult(MatchResult.BYE))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("BYE対局の結果は変更できない")
        void BYEの結果変更禁止() {
            Match byeMatch = Match.byeOf(1, 1, p1);
            assertThat(byeMatch.isBye()).isTrue();
            assertThat(byeMatch.pointsFor(p1)).isEqualTo(2);
            assertThatThrownBy(() -> byeMatch.withResult(MatchResult.PLAYER1_WIN))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("同一参加者同士・不正なBYE指定は作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> Match.pairOf(1, 1, p1, p1)).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new Match(MatchId.generate(), 1, 1, p1, null, MatchResult.NONE))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new Match(MatchId.generate(), 1, 1, p1, p2, MatchResult.BYE))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("対戦相手・関与判定が正しく動く")
        void 対戦相手の取得() {
            Match match = Match.pairOf(1, 1, p1, p2);
            assertThat(match.opponentOf(p1)).contains(p2);
            assertThat(match.opponentOf(p2)).contains(p1);
            assertThat(match.opponentOf(ParticipantId.generate())).isEmpty();
            assertThat(match.involves(p1)).isTrue();
            assertThat(match.involves(ParticipantId.generate())).isFalse();
            assertThat(match.pointsFor(ParticipantId.generate())).isZero();
        }
    }

    @Nested
    class ParticipantTest {

        @Test
        @DisplayName("棄権すると非アクティブになる")
        void 棄権() {
            Participant p = Participant.create("参加者一", "A社", Rank.DAN_3, 1);
            assertThat(p.isActive()).isTrue();
            assertThat(p.withdraw().isActive()).isFalse();
        }

        @Test
        @DisplayName("同一所属判定は所属未設定(null・空)ではfalseになる")
        void 同一所属判定() {
            Participant a1 = Participant.create("a", "A社", null, 1);
            Participant a2 = Participant.create("b", "A社", null, 2);
            Participant none1 = Participant.create("c", null, null, 3);
            Participant none2 = Participant.create("d", null, null, 4);
            assertThat(a1.hasSameOrganization(a2)).isTrue();
            assertThat(a1.hasSameOrganization(none1)).isFalse();
            assertThat(none1.hasSameOrganization(none2)).isFalse();
        }

        @Test
        @DisplayName("氏名なし・シード順0は作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> Participant.create(" ", null, null, 1))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Participant.create("x", null, null, 0))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class RankTest {

        @Test
        @DisplayName("棋力は20級が最弱・九段が最強で、1級の次は初段")
        void 棋力の順序() {
            assertThat(Rank.values()).hasSize(29);
            assertThat(Rank.values()[0]).isEqualTo(Rank.KYU_20);
            assertThat(Rank.values()[28]).isEqualTo(Rank.DAN_9);
            assertThat(Rank.DAN_1.isStrongerThan(Rank.KYU_1)).isTrue();
            assertThat(Rank.KYU_1.isStrongerThan(Rank.KYU_2)).isTrue();
            assertThat(Rank.DAN_9.isStrongerThan(Rank.DAN_8)).isTrue();
        }

        @Test
        @DisplayName("strongestFirstは強い順に並べ、未入力(null)を末尾に置く")
        void 強い順ソート() {
            List<Rank> ranks = new ArrayList<>(
                    Arrays.asList(Rank.KYU_20, null, Rank.DAN_9, Rank.DAN_1, Rank.KYU_1));
            ranks.sort(Rank.strongestFirst());
            assertThat(ranks).containsExactly(
                    Rank.DAN_9, Rank.DAN_1, Rank.KYU_1, Rank.KYU_20, null);
        }

        @Test
        @DisplayName("表示名: 級は数字、段は漢数字で初段のみ特別")
        void 表示名() {
            assertThat(Rank.KYU_20.displayName()).isEqualTo("20級");
            assertThat(Rank.KYU_1.displayName()).isEqualTo("1級");
            assertThat(Rank.DAN_1.displayName()).isEqualTo("初段");
            assertThat(Rank.DAN_9.displayName()).isEqualTo("九段");
        }
    }

    @Nested
    class IdTest {

        @Test
        @DisplayName("IDはULID形式(26文字)で生成され、空文字は拒否される")
        void ID生成とバリデーション() {
            assertThat(TournamentId.generate().value()).hasSize(26);
            assertThat(ParticipantId.generate().value()).hasSize(26);
            assertThat(MatchId.generate().value()).hasSize(26);
            assertThatThrownBy(() -> new TournamentId(" ")).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new ParticipantId("")).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new MatchId(null)).isInstanceOf(DomainException.class);
        }
    }
}
