package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;
import java.util.Optional;

/**
 * 対局。player2 が null の場合は不戦勝(BYE)を表す。
 * version は楽観ロック用(結果入力の競合検出。0 = 未保存)。
 * resultInputBy は結果を確定した主体(監査用。未確定・BYEは null。
 * OWNER = 運営者が直接確定、SHARE_TOKEN = 両者の自己申告が一致して自動確定)。
 * player1/2ReportedResult はトークン経由の自己申告(NONE = 未申告)。確定済み(OWNER・
 * SHARE_TOKENいずれも)の対局は、以後の自己申告で記録だけ更新され、確定値(result)には
 * 影響しない(確定が最終権威。食い違いの検知はDTO側で申告内容と確定結果を比較して行う)。
 * groupId は必須。対局は常にいずれかのグループに帰属する(05_swiss_pairing_algorithm.md §2.4)。
 */
public record Match(
        MatchId id,
        int roundNumber,
        int tableNumber,
        ParticipantId player1Id,
        ParticipantId player2Id,
        MatchResult result,
        ResultInputBy resultInputBy,
        MatchResult player1ReportedResult,
        MatchResult player2ReportedResult,
        long version,
        GroupId groupId) {

    public Match {
        if (player1Id == null) {
            throw new DomainException("player1は必須です");
        }
        if (player1Id.equals(player2Id)) {
            throw new DomainException("同一参加者同士の対局は作れません");
        }
        if (player2Id == null && result != MatchResult.BYE) {
            throw new DomainException("対戦相手なしの対局はBYEのみ許されます");
        }
        if (player2Id != null && result == MatchResult.BYE) {
            throw new DomainException("対戦相手がいる対局をBYEにはできません");
        }
        if (groupId == null) {
            throw new DomainException("対局の帰属グループは必須です");
        }
        if (player1ReportedResult == null) {
            player1ReportedResult = MatchResult.NONE;
        }
        if (player2ReportedResult == null) {
            player2ReportedResult = MatchResult.NONE;
        }
    }

    public static Match pairOf(
            int roundNumber, int tableNumber, ParticipantId p1, ParticipantId p2, GroupId groupId) {
        return new Match(
                MatchId.generate(), roundNumber, tableNumber, p1, p2, MatchResult.NONE, null,
                MatchResult.NONE, MatchResult.NONE, 0L, groupId);
    }

    public static Match byeOf(int roundNumber, int tableNumber, ParticipantId p1, GroupId groupId) {
        return new Match(
                MatchId.generate(), roundNumber, tableNumber, p1, null, MatchResult.BYE, null,
                MatchResult.NONE, MatchResult.NONE, 0L, groupId);
    }

    public boolean isBye() {
        return player2Id == null;
    }

    public boolean involves(ParticipantId participantId) {
        return player1Id.equals(participantId)
                || (player2Id != null && player2Id.equals(participantId));
    }

    public Optional<ParticipantId> opponentOf(ParticipantId participantId) {
        if (player1Id.equals(participantId)) {
            return Optional.ofNullable(player2Id);
        }
        if (player2Id != null && player2Id.equals(participantId)) {
            return Optional.of(player1Id);
        }
        return Optional.empty();
    }

    /** 指定参加者が得る勝点(2倍値)。未入力(NONE)は0 */
    public int pointsFor(ParticipantId participantId) {
        if (player1Id.equals(participantId)) {
            return result.pointsForPlayer1();
        }
        if (player2Id != null && player2Id.equals(participantId)) {
            return result.pointsForPlayer2();
        }
        return 0;
    }

    public Match withResult(MatchResult newResult) {
        return withResult(newResult, ResultInputBy.OWNER);
    }

    /** 運営者による直接確定。以後、参加者の自己申告(withReportedResult)ではresultを変更させない */
    public Match withResult(MatchResult newResult, ResultInputBy inputBy) {
        if (isBye()) {
            throw new DomainException("BYEの結果は変更できません");
        }
        if (newResult == MatchResult.BYE) {
            throw new DomainException("通常対局の結果をBYEにはできません");
        }
        return new Match(
                id, roundNumber, tableNumber, player1Id, player2Id, newResult, inputBy,
                player1ReportedResult, player2ReportedResult, version, groupId);
    }

    /**
     * トークン経由の自己申告(05_swiss_pairing_algorithm.mdの対象外・結果確定の運用ルール)。
     * side側の申告を記録し、両者の申告が一致すればresultを自動確定する。
     * 既に確定済み(resultが決定済み。運営者直接確定・自己申告一致の自動確定いずれも)の場合は
     * 申告の記録のみでresultは変えない(確定後の再申告でサイレントに結果が書き換わる/
     * 未確定へ巻き戻ることを防ぐ)。
     */
    public Match withReportedResult(MatchSide side, MatchResult claimed) {
        if (isBye()) {
            throw new DomainException("BYEの結果は変更できません");
        }
        if (claimed == MatchResult.NONE || claimed == MatchResult.BYE) {
            throw new DomainException("結果には勝敗・引き分け・両者負けのいずれかを指定してください");
        }
        MatchResult newP1 = side == MatchSide.PLAYER1 ? claimed : player1ReportedResult;
        MatchResult newP2 = side == MatchSide.PLAYER2 ? claimed : player2ReportedResult;

        if (result.isDecided()) {
            return new Match(
                    id, roundNumber, tableNumber, player1Id, player2Id, result, resultInputBy,
                    newP1, newP2, version, groupId);
        }
        boolean matched = newP1 != MatchResult.NONE && newP1 == newP2;
        MatchResult newResult = matched ? newP1 : MatchResult.NONE;
        ResultInputBy newInputBy = matched ? ResultInputBy.SHARE_TOKEN : null;
        return new Match(
                id, roundNumber, tableNumber, player1Id, player2Id, newResult, newInputBy,
                newP1, newP2, version, groupId);
    }

    /** ラウンド確定のブロック判定に使う。運営者・参加者のいずれも一切触れていない対局のみtrue */
    public boolean isUntouched() {
        return !result.isDecided()
                && player1ReportedResult == MatchResult.NONE
                && player2ReportedResult == MatchResult.NONE;
    }
}
