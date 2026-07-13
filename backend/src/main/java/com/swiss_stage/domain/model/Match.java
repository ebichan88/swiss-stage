package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;
import java.util.Optional;

/**
 * 対局。player2 が null の場合は不戦勝(BYE)を表す。
 * version は楽観ロック用(結果入力の競合検出。0 = 未保存)。
 * resultInputBy は結果を入力した主体(監査用。未入力・BYEは null)。
 */
public record Match(
        MatchId id,
        int roundNumber,
        int tableNumber,
        ParticipantId player1Id,
        ParticipantId player2Id,
        MatchResult result,
        ResultInputBy resultInputBy,
        long version) {

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
    }

    public static Match pairOf(int roundNumber, int tableNumber, ParticipantId p1, ParticipantId p2) {
        return new Match(
                MatchId.generate(), roundNumber, tableNumber, p1, p2, MatchResult.NONE, null, 0L);
    }

    public static Match byeOf(int roundNumber, int tableNumber, ParticipantId p1) {
        return new Match(
                MatchId.generate(), roundNumber, tableNumber, p1, null, MatchResult.BYE, null, 0L);
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

    public Match withResult(MatchResult newResult, ResultInputBy inputBy) {
        if (isBye()) {
            throw new DomainException("BYEの結果は変更できません");
        }
        if (newResult == MatchResult.BYE) {
            throw new DomainException("通常対局の結果をBYEにはできません");
        }
        return new Match(
                id, roundNumber, tableNumber, player1Id, player2Id, newResult, inputBy, version);
    }
}
