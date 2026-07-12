package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;
import java.time.Instant;

/**
 * 大会。状態遷移: PREPARING → IN_PROGRESS → FINISHED(逆行不可)。
 *
 * <p>shareToken は共有URL用トークン(未発行はnull)。
 * 時刻は Clock をDIした呼び出し側(application層)から渡す(domainでは Instant.now() を呼ばない)。
 */
public record Tournament(
        TournamentId id,
        String name,
        GameType gameType,
        int totalRounds,
        int currentRound,
        TournamentStatus status,
        Visibility visibility,
        String shareToken,
        String ownerSub,
        long version,
        Instant createdAt,
        Instant updatedAt) {

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
        if (createdAt == null || updatedAt == null) {
            throw new DomainException("大会の作成・更新日時は必須です");
        }
    }

    public static Tournament create(
            String name, GameType gameType, int totalRounds, String ownerSub, Instant now) {
        return new Tournament(
                TournamentId.generate(), name, gameType, totalRounds, 0,
                TournamentStatus.PREPARING, Visibility.PRIVATE, null, ownerSub, 0L, now, now);
    }

    public boolean isOwnedBy(String sub) {
        return ownerSub.equals(sub);
    }

    /** 大会開始(参加者確定後)。PREPARINGからのみ可 */
    public Tournament start() {
        if (status != TournamentStatus.PREPARING) {
            throw new DomainException("準備中の大会のみ開始できます");
        }
        return withStatus(TournamentStatus.IN_PROGRESS, currentRound);
    }

    /** 次ラウンドへ進む。開催中のみ可 */
    public Tournament advanceRound() {
        if (status != TournamentStatus.IN_PROGRESS) {
            throw new DomainException("開催中の大会のみラウンドを進められます");
        }
        if (currentRound >= totalRounds) {
            throw new DomainException("最終ラウンドを超えて進めることはできません");
        }
        return withStatus(status, currentRound + 1);
    }

    public Tournament finish() {
        if (status != TournamentStatus.IN_PROGRESS) {
            throw new DomainException("開催中の大会のみ終了できます");
        }
        return withStatus(TournamentStatus.FINISHED, currentRound);
    }

    public Tournament rename(String newName) {
        return new Tournament(id, newName, gameType, totalRounds, currentRound,
                status, visibility, shareToken, ownerSub, version, createdAt, updatedAt);
    }

    public Tournament withVisibility(Visibility newVisibility) {
        return new Tournament(id, name, gameType, totalRounds, currentRound,
                status, newVisibility, shareToken, ownerSub, version, createdAt, updatedAt);
    }

    public Tournament withShareToken(String newShareToken) {
        return new Tournament(id, name, gameType, totalRounds, currentRound,
                status, visibility, newShareToken, ownerSub, version, createdAt, updatedAt);
    }

    /** 保存直前に更新日時を刻む(application層がClockから渡す) */
    public Tournament touched(Instant now) {
        return new Tournament(id, name, gameType, totalRounds, currentRound,
                status, visibility, shareToken, ownerSub, version, createdAt, now);
    }

    private Tournament withStatus(TournamentStatus newStatus, int newCurrentRound) {
        return new Tournament(id, name, gameType, totalRounds, newCurrentRound,
                newStatus, visibility, shareToken, ownerSub, version, createdAt, updatedAt);
    }
}
