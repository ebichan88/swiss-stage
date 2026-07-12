package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;

/**
 * 大会。状態遷移: PREPARING → IN_PROGRESS → FINISHED(逆行不可)。
 */
public record Tournament(
        TournamentId id,
        String name,
        GameType gameType,
        int totalRounds,
        int currentRound,
        TournamentStatus status,
        Visibility visibility,
        String ownerSub,
        long version) {

    public Tournament {
        if (name == null || name.isBlank()) {
            throw new DomainException("大会名は必須です");
        }
        if (totalRounds < 1) {
            throw new DomainException("ラウンド数は1以上である必要があります");
        }
        if (currentRound < 0 || currentRound > totalRounds) {
            throw new DomainException("現在ラウンドが不正です");
        }
        if (ownerSub == null || ownerSub.isBlank()) {
            throw new DomainException("大会の所有者は必須です");
        }
    }

    public static Tournament create(String name, GameType gameType, int totalRounds, String ownerSub) {
        return new Tournament(
                TournamentId.generate(), name, gameType, totalRounds, 0,
                TournamentStatus.PREPARING, Visibility.PRIVATE, ownerSub, 0L);
    }

    public boolean isOwnedBy(String sub) {
        return ownerSub.equals(sub);
    }

    /** 大会開始(参加者確定後)。PREPARINGからのみ可 */
    public Tournament start() {
        if (status != TournamentStatus.PREPARING) {
            throw new DomainException("準備中の大会のみ開始できます");
        }
        return new Tournament(id, name, gameType, totalRounds, currentRound,
                TournamentStatus.IN_PROGRESS, visibility, ownerSub, version);
    }

    /** 次ラウンドへ進む。開催中のみ可 */
    public Tournament advanceRound() {
        if (status != TournamentStatus.IN_PROGRESS) {
            throw new DomainException("開催中の大会のみラウンドを進められます");
        }
        if (currentRound >= totalRounds) {
            throw new DomainException("最終ラウンドを超えて進めることはできません");
        }
        return new Tournament(id, name, gameType, totalRounds, currentRound + 1,
                status, visibility, ownerSub, version);
    }

    public Tournament finish() {
        if (status != TournamentStatus.IN_PROGRESS) {
            throw new DomainException("開催中の大会のみ終了できます");
        }
        return new Tournament(id, name, gameType, totalRounds, currentRound,
                TournamentStatus.FINISHED, visibility, ownerSub, version);
    }

    public Tournament withVisibility(Visibility newVisibility) {
        return new Tournament(id, name, gameType, totalRounds, currentRound,
                status, newVisibility, ownerSub, version);
    }
}
