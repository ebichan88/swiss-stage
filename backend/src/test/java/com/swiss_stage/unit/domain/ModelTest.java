package com.swiss_stage.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.BoardResult;
import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.MatchSide;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.ResultInputBy;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.RoundStatus;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamMatchId;
import com.swiss_stage.domain.model.TeamMember;
import com.swiss_stage.domain.model.TeamMemberId;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.model.Visibility;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModelTest {

    @Nested
    class TournamentTest {

        private final Instant now = Instant.parse("2026-07-13T00:00:00Z");
        private final Tournament preparing =
                Tournament.create(
                        "テスト大会", GameType.GO, CompetitionType.INDIVIDUAL, null, 5, "owner-sub", now);

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
            Tournament t = Tournament.create(
                            "大会", GameType.SHOGI, CompetitionType.INDIVIDUAL, null, 1, "owner", now)
                    .start().advanceRound();
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
        @DisplayName("名前変更・共有トークン設定・更新日時の付与ができる")
        void 属性の更新() {
            assertThat(preparing.rename("新名称").name()).isEqualTo("新名称");
            assertThat(preparing.withShareToken("token-123").shareToken()).isEqualTo("token-123");
            assertThat(preparing.shareToken()).isNull();

            Instant later = now.plusSeconds(60);
            Tournament touched = preparing.touched(later);
            assertThat(touched.updatedAt()).isEqualTo(later);
            assertThat(touched.createdAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("トークン経由の結果入力許可は既定でオフ、切り替えられる")
        void 結果入力許可の切り替え() {
            assertThat(preparing.resultInputEnabled()).isFalse();
            assertThat(preparing.withResultInputEnabled(true).resultInputEnabled()).isTrue();
        }

        @Test
        @DisplayName("大会名が空・ラウンド数0は作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> Tournament.create(
                            " ", GameType.GO, CompetitionType.INDIVIDUAL, null, 5, "owner", now))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Tournament.create(
                            "大会", GameType.GO, CompetitionType.INDIVIDUAL, null, 0, "owner", now))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("団体戦はteamSizeが3または5でないと作成できない")
        void 団体戦のチーム制検証() {
            assertThatThrownBy(() -> Tournament.create(
                            "団体戦", GameType.GO, CompetitionType.TEAM, null, 5, "owner", now))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Tournament.create(
                            "団体戦", GameType.GO, CompetitionType.TEAM, 4, 5, "owner", now))
                    .isInstanceOf(DomainException.class);

            Tournament threeTeam = Tournament.create(
                    "3チーム制大会", GameType.GO, CompetitionType.TEAM, 3, 5, "owner", now);
            assertThat(threeTeam.teamSize()).isEqualTo(3);
            assertThat(threeTeam.isTeamCompetition()).isTrue();

            Tournament fiveTeam = Tournament.create(
                    "5チーム制大会", GameType.GO, CompetitionType.TEAM, 5, 5, "owner", now);
            assertThat(fiveTeam.teamSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("個人戦にteamSizeは指定できない")
        void 個人戦のteamSize指定禁止() {
            assertThatThrownBy(() -> Tournament.create(
                            "個人戦", GameType.GO, CompetitionType.INDIVIDUAL, 3, 5, "owner", now))
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
        private final GroupId groupId = GroupId.generate();

        @Test
        @DisplayName("結果の入力・上書きができ、BYEへの変更はできない")
        void 結果入力() {
            Match match = Match.pairOf(1, 1, p1, p2, groupId);
            assertThat(match.resultInputBy()).isNull();
            Match decided = match.withResult(MatchResult.PLAYER1_WIN);
            assertThat(decided.pointsFor(p1)).isEqualTo(2);
            assertThat(decided.pointsFor(p2)).isZero();
            assertThat(decided.resultInputBy()).isEqualTo(ResultInputBy.OWNER);
            assertThat(match.withResult(MatchResult.DRAW, ResultInputBy.SHARE_TOKEN)
                    .resultInputBy()).isEqualTo(ResultInputBy.SHARE_TOKEN);
            assertThatThrownBy(() -> match.withResult(MatchResult.BYE))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("BYE対局の結果は変更できない")
        void BYEの結果変更禁止() {
            Match byeMatch = Match.byeOf(1, 1, p1, groupId);
            assertThat(byeMatch.isBye()).isTrue();
            assertThat(byeMatch.pointsFor(p1)).isEqualTo(2);
            assertThatThrownBy(() -> byeMatch.withResult(MatchResult.PLAYER1_WIN))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("同一参加者同士・不正なBYE指定・グループなしは作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> Match.pairOf(1, 1, p1, p1, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(
                    () -> new Match(
                            MatchId.generate(), 1, 1, p1, null, MatchResult.NONE, null,
                            MatchResult.NONE, MatchResult.NONE, 0L, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(
                    () -> new Match(
                            MatchId.generate(), 1, 1, p1, p2, MatchResult.BYE, null,
                            MatchResult.NONE, MatchResult.NONE, 0L, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Match.pairOf(1, 1, p1, p2, null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("両者の自己申告が一致すると自動確定し、一致しない間は未確定のまま")
        void 自己申告の一致で自動確定() {
            Match match = Match.pairOf(1, 1, p1, p2, groupId);
            assertThat(match.isUntouched()).isTrue();

            Match waiting = match.withReportedResult(MatchSide.PLAYER1, MatchResult.PLAYER1_WIN);
            assertThat(waiting.result()).isEqualTo(MatchResult.NONE);
            assertThat(waiting.player1ReportedResult()).isEqualTo(MatchResult.PLAYER1_WIN);
            assertThat(waiting.player2ReportedResult()).isEqualTo(MatchResult.NONE);
            assertThat(waiting.isUntouched()).isFalse();

            Match conflicting =
                    waiting.withReportedResult(MatchSide.PLAYER2, MatchResult.PLAYER2_WIN);
            assertThat(conflicting.result()).isEqualTo(MatchResult.NONE);
            assertThat(conflicting.resultInputBy()).isNull();

            Match matched = conflicting.withReportedResult(MatchSide.PLAYER2, MatchResult.PLAYER1_WIN);
            assertThat(matched.result()).isEqualTo(MatchResult.PLAYER1_WIN);
            assertThat(matched.resultInputBy()).isEqualTo(ResultInputBy.SHARE_TOKEN);
        }

        @Test
        @DisplayName("運営者が直接確定した結果は、その後の自己申告(一致・不一致とも)で上書きされない")
        void 運営者確定は自己申告で上書きされない() {
            Match ownerDecided = Match.pairOf(1, 1, p1, p2, groupId)
                    .withResult(MatchResult.DRAW, ResultInputBy.OWNER);

            Match afterReport = ownerDecided
                    .withReportedResult(MatchSide.PLAYER1, MatchResult.PLAYER1_WIN)
                    .withReportedResult(MatchSide.PLAYER2, MatchResult.PLAYER1_WIN);

            assertThat(afterReport.result()).isEqualTo(MatchResult.DRAW);
            assertThat(afterReport.resultInputBy()).isEqualTo(ResultInputBy.OWNER);
            // 申告自体は記録として残る
            assertThat(afterReport.player1ReportedResult()).isEqualTo(MatchResult.PLAYER1_WIN);
            assertThat(afterReport.player2ReportedResult()).isEqualTo(MatchResult.PLAYER1_WIN);
        }

        @Test
        @DisplayName("自己申告一致で自動確定した結果も、その後の自己申告の変更で上書き・巻き戻りしない")
        void 自動確定は自己申告の変更で上書きされない() {
            Match autoDecided = Match.pairOf(1, 1, p1, p2, groupId)
                    .withReportedResult(MatchSide.PLAYER1, MatchResult.PLAYER1_WIN)
                    .withReportedResult(MatchSide.PLAYER2, MatchResult.PLAYER1_WIN);
            assertThat(autoDecided.result()).isEqualTo(MatchResult.PLAYER1_WIN);
            assertThat(autoDecided.resultInputBy()).isEqualTo(ResultInputBy.SHARE_TOKEN);

            // 片方が申告を変え、もう一方の申告と食い違っても結果は変わらない(未確定への巻き戻りもしない)
            Match afterChange =
                    autoDecided.withReportedResult(MatchSide.PLAYER1, MatchResult.PLAYER2_WIN);
            assertThat(afterChange.result()).isEqualTo(MatchResult.PLAYER1_WIN);
            assertThat(afterChange.resultInputBy()).isEqualTo(ResultInputBy.SHARE_TOKEN);
            assertThat(afterChange.player1ReportedResult()).isEqualTo(MatchResult.PLAYER2_WIN);
            assertThat(afterChange.player2ReportedResult()).isEqualTo(MatchResult.PLAYER1_WIN);

            // 両者が同じ新しい値に申告を変えても、確定結果は書き換わらない
            Match afterBothChange =
                    afterChange.withReportedResult(MatchSide.PLAYER2, MatchResult.PLAYER2_WIN);
            assertThat(afterBothChange.result()).isEqualTo(MatchResult.PLAYER1_WIN);
            assertThat(afterBothChange.resultInputBy()).isEqualTo(ResultInputBy.SHARE_TOKEN);
        }

        @Test
        @DisplayName("自己申告にNONE・BYEは指定できず、BYE対局には自己申告できない")
        void 自己申告のバリデーション() {
            Match match = Match.pairOf(1, 1, p1, p2, groupId);
            assertThatThrownBy(() -> match.withReportedResult(MatchSide.PLAYER1, MatchResult.NONE))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> match.withReportedResult(MatchSide.PLAYER1, MatchResult.BYE))
                    .isInstanceOf(DomainException.class);
            Match byeMatch = Match.byeOf(1, 1, p1, groupId);
            assertThatThrownBy(
                    () -> byeMatch.withReportedResult(MatchSide.PLAYER1, MatchResult.PLAYER1_WIN))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("対戦相手・関与判定が正しく動く")
        void 対戦相手の取得() {
            Match match = Match.pairOf(1, 1, p1, p2, groupId);
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

        private final GroupId groupId = GroupId.generate();

        @Test
        @DisplayName("棄権すると非アクティブになる")
        void 棄権() {
            Participant p = Participant.create("参加者一", "A社", Rank.DAN_3, 1, groupId);
            assertThat(p.isActive()).isTrue();
            assertThat(p.withdraw().isActive()).isFalse();
        }

        @Test
        @DisplayName("同一所属判定は所属未設定(null・空)ではfalseになる")
        void 同一所属判定() {
            Participant a1 = Participant.create("a", "A社", null, 1, groupId);
            Participant a2 = Participant.create("b", "A社", null, 2, groupId);
            Participant none1 = Participant.create("c", null, null, 3, groupId);
            Participant none2 = Participant.create("d", null, null, 4, groupId);
            assertThat(a1.hasSameOrganization(a2)).isTrue();
            assertThat(a1.hasSameOrganization(none1)).isFalse();
            assertThat(none1.hasSameOrganization(none2)).isFalse();
        }

        @Test
        @DisplayName("氏名なし・エントリー順0・グループなしは作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> Participant.create(" ", null, null, 1, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Participant.create("x", null, null, 0, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Participant.create("x", null, null, 1, null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("グループ割当は変更でき、棄権しても保たれる。未割当には戻せない")
        void グループ割当() {
            GroupId other = GroupId.generate();
            Participant p = Participant.create("参加者一", null, null, 1, groupId);
            assertThat(p.groupId()).isEqualTo(groupId);
            Participant moved = p.withGroup(other);
            assertThat(moved.groupId()).isEqualTo(other);
            assertThat(moved.withdraw().groupId()).isEqualTo(other);
            assertThatThrownBy(() -> p.withGroup(null)).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class GroupTest {

        @Test
        @DisplayName("グループを作成・改名できる")
        void 作成と改名() {
            Group group = Group.create("A");
            assertThat(group.name()).isEqualTo("A");
            Group renamed = group.rename("Aクラス");
            assertThat(renamed.id()).isEqualTo(group.id());
            assertThat(renamed.name()).isEqualTo("Aクラス");
        }

        @Test
        @DisplayName("グループ名なし・50文字超は作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> Group.create(" ")).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Group.create("あ".repeat(51)))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("GroupIdは連続生成しても昇順になる(作成順ソートの前提)")
        void 採番の単調増加() {
            GroupId first = GroupId.generate();
            GroupId second = GroupId.generate();
            assertThat(first.value()).isLessThan(second.value());
        }
    }

    @Nested
    class RankTest {

        @Test
        @DisplayName("棋力は20級が最弱・9段が最強で、1級の次は初段")
        void 棋力の順序() {
            assertThat(Rank.values()).hasSize(29);
            assertThat(Rank.DAN_1.isStrongerThan(Rank.KYU_1)).isTrue();
            assertThat(Rank.KYU_1.isStrongerThan(Rank.KYU_2)).isTrue();
            assertThat(Rank.DAN_9.isStrongerThan(Rank.DAN_8)).isTrue();
            assertThat(Rank.KYU_20.sortOrder()).isEqualTo(20);
            assertThat(Rank.DAN_9.sortOrder()).isEqualTo(-9);
        }

        @Test
        @DisplayName("sortOrderは重複せず、宣言順(弱い順)とも一致している")
        void sortOrderの整合() {
            Rank[] ranks = Rank.values();
            for (int i = 1; i < ranks.length; i++) {
                // 宣言が後ろのものほど強い(sortOrderが小さい)こと。重複もここで検出される
                assertThat(ranks[i].sortOrder())
                        .as("%s は %s より強く宣言されている", ranks[i], ranks[i - 1])
                        .isLessThan(ranks[i - 1].sortOrder());
            }
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
        @DisplayName("表示名からパースできる(初段='1段'も許容、未知はempty)")
        void 表示名からのパース() {
            assertThat(Rank.fromDisplayName("20級")).contains(Rank.KYU_20);
            assertThat(Rank.fromDisplayName("初段")).contains(Rank.DAN_1);
            assertThat(Rank.fromDisplayName("1段")).contains(Rank.DAN_1);
            assertThat(Rank.fromDisplayName(" 3段 ")).contains(Rank.DAN_3);
            assertThat(Rank.fromDisplayName("")).isEmpty();
            assertThat(Rank.fromDisplayName(null)).isEmpty();
            assertThat(Rank.fromDisplayName("十段")).isEmpty();
            assertThat(Rank.fromDisplayName("21級")).isEmpty();
        }

        @Test
        @DisplayName("表示名は算用数字で、初段のみ特別")
        void 表示名() {
            assertThat(Rank.KYU_20.displayName()).isEqualTo("20級");
            assertThat(Rank.KYU_1.displayName()).isEqualTo("1級");
            assertThat(Rank.DAN_1.displayName()).isEqualTo("初段");
            assertThat(Rank.DAN_2.displayName()).isEqualTo("2段");
            assertThat(Rank.DAN_9.displayName()).isEqualTo("9段");
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
            assertThat(TeamId.generate().value()).hasSize(26);
            assertThat(TeamMemberId.generate().value()).hasSize(26);
            assertThat(TeamMatchId.generate().value()).hasSize(26);
            assertThatThrownBy(() -> new TournamentId(" ")).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new ParticipantId("")).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new MatchId(null)).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new TeamId(" ")).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new TeamMemberId("")).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> new TeamMatchId(null)).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class TeamMemberTest {

        @Test
        @DisplayName("boardPositionの有無で正メンバー・補欠を判定する")
        void 正メンバーと補欠() {
            TeamMember captain = TeamMember.create("主将 一郎", Rank.DAN_3, 1);
            assertThat(captain.isReserve()).isFalse();
            TeamMember reserve = TeamMember.create("補欠 次郎", null, null);
            assertThat(reserve.isReserve()).isTrue();
        }

        @Test
        @DisplayName("氏名なし・50文字超・ボード位置0以下は作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> TeamMember.create(" ", null, 1))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> TeamMember.create("あ".repeat(51), null, 1))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> TeamMember.create("三郎", null, 0))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class TeamTest {

        private final GroupId groupId = GroupId.generate();

        @Test
        @DisplayName("棄権すると非アクティブになり、グループ割当・改名ができる")
        void 状態遷移と属性変更() {
            Team team = Team.create("Aチーム", 1, groupId);
            assertThat(team.isActive()).isTrue();
            assertThat(team.withdraw().isActive()).isFalse();
            assertThat(team.rename("Bチーム").name()).isEqualTo("Bチーム");

            GroupId other = GroupId.generate();
            assertThat(team.withGroup(other).groupId()).isEqualTo(other);
        }

        @Test
        @DisplayName("メンバーの追加・削除・置き換えができる")
        void メンバー操作() {
            Team team = Team.create("Aチーム", 1, groupId);
            TeamMember captain = TeamMember.create("主将 一郎", Rank.DAN_3, 1);
            TeamMember reserve = TeamMember.create("補欠 次郎", null, null);

            Team withMembers = team.withMember(captain).withMember(reserve);
            assertThat(withMembers.members()).containsExactly(captain, reserve);
            assertThat(withMembers.reserveCount()).isEqualTo(1);

            TeamMember renamedCaptain = new TeamMember(captain.id(), "主将 三郎", Rank.DAN_2, 1);
            Team replaced = withMembers.withReplacedMember(captain.id(), renamedCaptain);
            assertThat(replaced.members()).containsExactly(renamedCaptain, reserve);

            Team removed = replaced.withoutMember(reserve.id());
            assertThat(removed.members()).containsExactly(renamedCaptain);
        }

        @Test
        @DisplayName("同一チーム内でボード位置が重複するメンバー構成は作成できない")
        void ボード位置重複の禁止() {
            TeamMember first = TeamMember.create("一郎", null, 1);
            TeamMember duplicate = TeamMember.create("次郎", null, 1);
            assertThatThrownBy(
                    () -> new Team(TeamId.generate(), "Aチーム", 1, ParticipantStatus.ACTIVE, groupId,
                            List.of(first, duplicate)))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("チーム名なし・50文字超・エントリー順0・グループなしは作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> Team.create(" ", 1, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Team.create("あ".repeat(51), 1, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Team.create("Aチーム", 0, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> Team.create("Aチーム", 1, null))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class BoardResultTest {

        @Test
        @DisplayName("運営者の直接確定ができ、BYEは指定できない")
        void 直接確定() {
            BoardResult board = BoardResult.unplayed(1);
            BoardResult decided = board.withResult(MatchResult.PLAYER1_WIN);
            assertThat(decided.pointsForTeam1()).isEqualTo(2);
            assertThat(decided.pointsForTeam2()).isZero();
            assertThatThrownBy(() -> board.withResult(MatchResult.BYE))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(
                    () -> new BoardResult(1, MatchResult.BYE, MatchResult.NONE, MatchResult.NONE))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("両者の自己申告が一致すると自動確定し、一致しない間は未確定のまま")
        void 自己申告の一致で自動確定() {
            BoardResult board = BoardResult.unplayed(1);
            assertThat(board.isUntouched()).isTrue();

            BoardResult waiting = board.withReportedResult(MatchSide.PLAYER1, MatchResult.PLAYER1_WIN);
            assertThat(waiting.result()).isEqualTo(MatchResult.NONE);
            assertThat(waiting.isUntouched()).isFalse();

            BoardResult conflicting =
                    waiting.withReportedResult(MatchSide.PLAYER2, MatchResult.PLAYER2_WIN);
            assertThat(conflicting.result()).isEqualTo(MatchResult.NONE);

            BoardResult matched =
                    conflicting.withReportedResult(MatchSide.PLAYER2, MatchResult.PLAYER1_WIN);
            assertThat(matched.result()).isEqualTo(MatchResult.PLAYER1_WIN);
        }

        @Test
        @DisplayName("確定済みの結果はその後の自己申告で上書きされない")
        void 確定後は自己申告で上書きされない() {
            BoardResult decided = BoardResult.unplayed(1).withResult(MatchResult.DRAW);
            BoardResult afterReport =
                    decided.withReportedResult(MatchSide.PLAYER1, MatchResult.PLAYER1_WIN);
            assertThat(afterReport.result()).isEqualTo(MatchResult.DRAW);
            assertThat(afterReport.team1ReportedResult()).isEqualTo(MatchResult.PLAYER1_WIN);
        }

        @Test
        @DisplayName("自己申告にNONE・BYEは指定できない")
        void 自己申告のバリデーション() {
            BoardResult board = BoardResult.unplayed(1);
            assertThatThrownBy(() -> board.withReportedResult(MatchSide.PLAYER1, MatchResult.NONE))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> board.withReportedResult(MatchSide.PLAYER1, MatchResult.BYE))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    class TeamMatchTest {

        private final TeamId team1 = TeamId.generate();
        private final TeamId team2 = TeamId.generate();
        private final GroupId groupId = GroupId.generate();

        @Test
        @DisplayName("3チーム制のペアは3ボード分未入力で生成される")
        void ペア生成() {
            TeamMatch match = TeamMatch.pairOf(1, 1, team1, team2, 3, groupId);
            assertThat(match.boardResults()).hasSize(3);
            assertThat(match.isUntouched()).isTrue();
            assertThat(match.isBye()).isFalse();
            assertThat(match.involves(team1)).isTrue();
            assertThat(match.opponentOf(team1)).contains(team2);
            assertThat(match.opponentOf(TeamId.generate())).isEmpty();
        }

        @Test
        @DisplayName("BYEは対局を1件も持たず、team1に満点が入る")
        void BYE生成() {
            TeamMatch bye = TeamMatch.byeOf(1, 1, team1, groupId);
            assertThat(bye.isBye()).isTrue();
            assertThat(bye.boardResults()).isEmpty();
            assertThat(bye.pointsFor(team1)).isEqualTo(2);
            assertThat(bye.pointsFor(team2)).isZero();
            assertThat(bye.isUntouched()).isFalse();
            assertThatThrownBy(
                    () -> bye.withBoardResults(List.of(MatchResult.PLAYER1_WIN)))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("全ボード決着で勝敗(2/0)、同点なら分(1)が両チームに入る")
        void 全ボード決着時の勝点導出() {
            TeamMatch match = TeamMatch.pairOf(1, 1, team1, team2, 3, groupId)
                    .withBoardResults(List.of(
                            MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER2_WIN));
            assertThat(match.boardPointsFor(team1)).isEqualTo(4);
            assertThat(match.boardPointsFor(team2)).isEqualTo(2);
            assertThat(match.pointsFor(team1)).isEqualTo(2);
            assertThat(match.pointsFor(team2)).isZero();

            TeamMatch drawn = TeamMatch.pairOf(1, 1, team1, team2, 3, groupId)
                    .withBoardResults(List.of(
                            MatchResult.PLAYER1_WIN, MatchResult.PLAYER2_WIN, MatchResult.DRAW));
            assertThat(drawn.pointsFor(team1)).isEqualTo(1);
            assertThat(drawn.pointsFor(team2)).isEqualTo(1);
        }

        @Test
        @DisplayName("一部ボードのみ決着している間は順位計算上の勝点は0のまま")
        void 部分決着は未確定扱い() {
            TeamMatch partial = TeamMatch.pairOf(1, 1, team1, team2, 3, groupId)
                    .withBoardResults(List.of(
                            MatchResult.PLAYER1_WIN, MatchResult.NONE, MatchResult.NONE));
            assertThat(partial.isFullyDecided()).isFalse();
            assertThat(partial.pointsFor(team1)).isZero();
            assertThat(partial.pointsFor(team2)).isZero();
            assertThat(partial.isUntouched()).isFalse();
        }

        @Test
        @DisplayName("自己申告はボード単位で独立に確定し、他ボードの不一致に影響されない")
        void ボード単位の自己申告() {
            TeamMatch match = TeamMatch.pairOf(1, 1, team1, team2, 3, groupId)
                    .withReportedBoardResults(MatchSide.PLAYER1, List.of(
                            MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.DRAW))
                    .withReportedBoardResults(MatchSide.PLAYER2, List.of(
                            MatchResult.PLAYER1_WIN, MatchResult.PLAYER2_WIN, MatchResult.DRAW));

            assertThat(match.boardResults().get(0).result()).isEqualTo(MatchResult.PLAYER1_WIN);
            assertThat(match.boardResults().get(1).result()).isEqualTo(MatchResult.NONE);
            assertThat(match.boardResults().get(2).result()).isEqualTo(MatchResult.DRAW);
            assertThat(match.resultInputBy()).isEqualTo(ResultInputBy.SHARE_TOKEN);
        }

        @Test
        @DisplayName("同一チーム同士・グループなし・ボード数不一致は作成できない")
        void バリデーション() {
            assertThatThrownBy(() -> TeamMatch.pairOf(1, 1, team1, team1, 3, groupId))
                    .isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> TeamMatch.pairOf(1, 1, team1, team2, 3, null))
                    .isInstanceOf(DomainException.class);
            TeamMatch match = TeamMatch.pairOf(1, 1, team1, team2, 3, groupId);
            assertThatThrownBy(() -> match.withBoardResults(
                            List.of(MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN)))
                    .isInstanceOf(DomainException.class);
        }
    }
}
