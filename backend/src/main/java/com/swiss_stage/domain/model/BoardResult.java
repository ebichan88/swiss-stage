package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;

/**
 * 主将戦・副将戦…の1ボード分の結果(値オブジェクト。TeamMatchに埋め込み)。
 * resultはteam1視点で既存MatchResultを再利用する(NONE=未入力。BYEは不可)。
 * team1ReportedResult/team2ReportedResultは共有トークン経由の自己申告(NONE=未申告)。
 * 個人戦のMatch.withReportedResultと同じ突き合わせルールをボード単位で独立に適用する
 * (05_swiss_pairing_algorithm.md §5.4)。MatchSide.PLAYER1/PLAYER2をteam1/team2に読み替える。
 */
public record BoardResult(
        int boardPosition,
        MatchResult result,
        MatchResult team1ReportedResult,
        MatchResult team2ReportedResult) {

    public BoardResult {
        if (boardPosition < 1) {
            throw new DomainException("ボード位置は1以上である必要があります");
        }
        if (result == MatchResult.BYE) {
            throw new DomainException("ボード結果にBYEは指定できません");
        }
        if (team1ReportedResult == null) {
            team1ReportedResult = MatchResult.NONE;
        }
        if (team2ReportedResult == null) {
            team2ReportedResult = MatchResult.NONE;
        }
    }

    public static BoardResult unplayed(int boardPosition) {
        return new BoardResult(boardPosition, MatchResult.NONE, MatchResult.NONE, MatchResult.NONE);
    }

    /** 運営者による直接確定 */
    public BoardResult withResult(MatchResult newResult) {
        if (newResult == MatchResult.BYE) {
            throw new DomainException("ボード結果にBYEは指定できません");
        }
        return new BoardResult(boardPosition, newResult, team1ReportedResult, team2ReportedResult);
    }

    /**
     * トークン経由の自己申告。side側の申告を記録し、両者の申告が一致すればresultを自動確定する。
     * 既に確定済みの場合は申告の記録のみでresultは変えない(Match.withReportedResultと同じ規則)。
     */
    public BoardResult withReportedResult(MatchSide side, MatchResult claimed) {
        if (claimed == MatchResult.NONE || claimed == MatchResult.BYE) {
            throw new DomainException("結果には勝敗・引き分け・両者負けのいずれかを指定してください");
        }
        MatchResult newTeam1 = side == MatchSide.PLAYER1 ? claimed : team1ReportedResult;
        MatchResult newTeam2 = side == MatchSide.PLAYER2 ? claimed : team2ReportedResult;

        if (result.isDecided()) {
            return new BoardResult(boardPosition, result, newTeam1, newTeam2);
        }
        boolean matched = newTeam1 != MatchResult.NONE && newTeam1 == newTeam2;
        MatchResult newResult = matched ? newTeam1 : MatchResult.NONE;
        return new BoardResult(boardPosition, newResult, newTeam1, newTeam2);
    }

    public boolean isUntouched() {
        return !result.isDecided()
                && team1ReportedResult == MatchResult.NONE
                && team2ReportedResult == MatchResult.NONE;
    }

    /** team1が得る勝点(2倍値)。未入力(NONE)は0 */
    public int pointsForTeam1() {
        return result.pointsForPlayer1();
    }

    /** team2が得る勝点(2倍値)。未入力(NONE)は0 */
    public int pointsForTeam2() {
        return result.pointsForPlayer2();
    }
}
