package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * 団体戦のチーム対チーム対局。team2Id が null の場合は不戦勝(BYE。boardResultsは空)。
 * チーム全体の勝敗は単一のenumとして保存せず、boardResultsの各ボード点数(2倍値)の合計
 * (teamPoints)を都度比較して導出する(05_swiss_pairing_algorithm.md §5.3)。
 * version はTeamMatch単位の楽観ロック(ボード配列をまとめて1回の更新で書き換える設計のため、
 * ボードごとのversionは持たない)。groupId は必須(個人戦のMatchと同じくグループに帰属する)。
 */
public record TeamMatch(
        TeamMatchId id,
        int roundNumber,
        int tableNumber,
        TeamId team1Id,
        TeamId team2Id,
        List<BoardResult> boardResults,
        ResultInputBy resultInputBy,
        long version,
        GroupId groupId) {

    public TeamMatch {
        if (team1Id == null) {
            throw new DomainException("team1は必須です");
        }
        if (team1Id.equals(team2Id)) {
            throw new DomainException("同一チーム同士の対局は作れません");
        }
        if (team2Id == null && !boardResults.isEmpty()) {
            throw new DomainException("不戦勝の対局にボード結果は持てません");
        }
        if (team2Id != null && boardResults.isEmpty()) {
            throw new DomainException("対戦相手がいる対局にはボード結果が必要です");
        }
        if (groupId == null) {
            throw new DomainException("対局の帰属グループは必須です");
        }
        boardResults = List.copyOf(boardResults);
    }

    public static TeamMatch pairOf(
            int roundNumber, int tableNumber, TeamId team1Id, TeamId team2Id, int teamSize,
            GroupId groupId) {
        List<BoardResult> boards = IntStream.rangeClosed(1, teamSize)
                .mapToObj(BoardResult::unplayed)
                .toList();
        return new TeamMatch(
                TeamMatchId.generate(), roundNumber, tableNumber, team1Id, team2Id, boards, null,
                0L, groupId);
    }

    public static TeamMatch byeOf(
            int roundNumber, int tableNumber, TeamId team1Id, GroupId groupId) {
        return new TeamMatch(
                TeamMatchId.generate(), roundNumber, tableNumber, team1Id, null, List.of(), null,
                0L, groupId);
    }

    public boolean isBye() {
        return team2Id == null;
    }

    public boolean involves(TeamId teamId) {
        return team1Id.equals(teamId) || (team2Id != null && team2Id.equals(teamId));
    }

    public Optional<TeamId> opponentOf(TeamId teamId) {
        if (team1Id.equals(teamId)) {
            return Optional.ofNullable(team2Id);
        }
        if (team2Id != null && team2Id.equals(teamId)) {
            return Optional.of(team1Id);
        }
        return Optional.empty();
    }

    /** 全ボードが決着済み(BYEも含む)か。順位計算(標準の勝点付与)の対象にできるかの判定に使う */
    public boolean isFullyDecided() {
        return isBye() || boardResults.stream().allMatch(b -> b.result().isDecided());
    }

    /** 指定チームのボード点数の合計(2倍値)。表示用の内訳(例: 3-0)に使う。未入力ボードは0扱い */
    public int boardPointsFor(TeamId teamId) {
        if (team1Id.equals(teamId)) {
            return boardResults.stream().mapToInt(BoardResult::pointsForTeam1).sum();
        }
        if (team2Id != null && team2Id.equals(teamId)) {
            return boardResults.stream().mapToInt(BoardResult::pointsForTeam2).sum();
        }
        return 0;
    }

    /**
     * 順位計算用の勝点(2倍値。勝=2,分=1,負=0)。個人戦のMatch.pointsForと同じスケールにするため、
     * ボード点数の合計そのものではなく、両チームのboardPointsForを比較して導出する
     * (05_swiss_pairing_algorithm.md §5.3)。全ボード決着前はNONE扱いで0を返す
     */
    public int pointsFor(TeamId teamId) {
        if (isBye()) {
            return team1Id.equals(teamId) ? 2 : 0;
        }
        if (!isFullyDecided()) {
            return 0;
        }
        int mine = boardPointsFor(teamId);
        int opponentPoints = boardPointsFor(team1Id.equals(teamId) ? team2Id : team1Id);
        if (mine > opponentPoints) {
            return 2;
        }
        return mine == opponentPoints ? 1 : 0;
    }

    /** 運営者による直接確定。ボード配列をまとめて置き換える(NONEを含めた部分入力も許す) */
    public TeamMatch withBoardResults(List<MatchResult> newResults) {
        if (isBye()) {
            throw new DomainException("BYEの結果は変更できません");
        }
        if (newResults.size() != boardResults.size()) {
            throw new DomainException("ボード結果の数がチーム制と一致しません");
        }
        List<BoardResult> updated = IntStream.range(0, boardResults.size())
                .mapToObj(i -> boardResults.get(i).withResult(newResults.get(i)))
                .toList();
        return new TeamMatch(
                id, roundNumber, tableNumber, team1Id, team2Id, updated, ResultInputBy.OWNER,
                version, groupId);
    }

    /** トークン経由の自己申告。ボード単位で独立に確定判定する(05 §5.4) */
    public TeamMatch withReportedBoardResults(MatchSide side, List<MatchResult> claimed) {
        if (isBye()) {
            throw new DomainException("BYEの結果は変更できません");
        }
        if (claimed.size() != boardResults.size()) {
            throw new DomainException("ボード結果の数がチーム制と一致しません");
        }
        List<BoardResult> updated = IntStream.range(0, boardResults.size())
                .mapToObj(i -> boardResults.get(i).withReportedResult(side, claimed.get(i)))
                .toList();
        boolean anyNewlyDecided = IntStream.range(0, boardResults.size())
                .anyMatch(i -> !boardResults.get(i).result().isDecided()
                        && updated.get(i).result().isDecided());
        ResultInputBy newInputBy = anyNewlyDecided ? ResultInputBy.SHARE_TOKEN : resultInputBy;
        return new TeamMatch(
                id, roundNumber, tableNumber, team1Id, team2Id, updated, newInputBy, version, groupId);
    }

    /** ラウンド確定のブロック判定。運営者・参加者のいずれも一切触れていない対局のみtrue */
    public boolean isUntouched() {
        return !isBye() && boardResults.stream().allMatch(BoardResult::isUntouched);
    }
}
