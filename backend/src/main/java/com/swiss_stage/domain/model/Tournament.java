package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;
import java.time.Instant;
import java.util.Set;

/**
 * 大会。状態遷移: PREPARING → IN_PROGRESS → FINISHED(逆行不可)。
 *
 * <p>shareToken は共有URL用トークン(未発行はnull)。
 * resultInputEnabled は共有トークン経由の結果入力を許可するか(13_security_design.md §3)。
 * competitionType/teamSize は作成後変更不可(totalRoundsと同様)。teamSizeは
 * competitionType=TEAM の時のみ非null(3または5。05_swiss_pairing_algorithm.md §5.1)。
 * 時刻は Clock をDIした呼び出し側(application層)から渡す(domainでは Instant.now() を呼ばない)。
 */
public record Tournament(
        TournamentId id,
        String name,
        GameType gameType,
        CompetitionType competitionType,
        Integer teamSize,
        int totalRounds,
        int currentRound,
        TournamentStatus status,
        Visibility visibility,
        String shareToken,
        boolean resultInputEnabled,
        String ownerSub,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    private static final Set<Integer> VALID_TEAM_SIZES = Set.of(3, 5);

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
        if (competitionType == CompetitionType.TEAM
                && (teamSize == null || !VALID_TEAM_SIZES.contains(teamSize))) {
            throw new DomainException("団体戦のチーム制は3人制または5人制である必要があります");
        }
        if (competitionType == CompetitionType.INDIVIDUAL && teamSize != null) {
            throw new DomainException("個人戦にチーム制は指定できません");
        }
    }

    public static Tournament create(
            String name,
            GameType gameType,
            CompetitionType competitionType,
            Integer teamSize,
            int totalRounds,
            String ownerSub,
            Instant now) {
        return new Tournament(
                TournamentId.generate(), name, gameType, competitionType, teamSize, totalRounds, 0,
                TournamentStatus.PREPARING, Visibility.PRIVATE, null, false, ownerSub, 0L, now, now);
    }

    public boolean isTeamCompetition() {
        return competitionType == CompetitionType.TEAM;
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
        return new Tournament(id, newName, gameType, competitionType, teamSize, totalRounds,
                currentRound, status, visibility, shareToken, resultInputEnabled, ownerSub, version,
                createdAt, updatedAt);
    }

    public Tournament withVisibility(Visibility newVisibility) {
        return new Tournament(id, name, gameType, competitionType, teamSize, totalRounds,
                currentRound, status, newVisibility, shareToken, resultInputEnabled, ownerSub, version,
                createdAt, updatedAt);
    }

    public Tournament withShareToken(String newShareToken) {
        return new Tournament(id, name, gameType, competitionType, teamSize, totalRounds,
                currentRound, status, visibility, newShareToken, resultInputEnabled, ownerSub, version,
                createdAt, updatedAt);
    }

    public Tournament withResultInputEnabled(boolean newResultInputEnabled) {
        return new Tournament(id, name, gameType, competitionType, teamSize, totalRounds,
                currentRound, status, visibility, shareToken, newResultInputEnabled, ownerSub, version,
                createdAt, updatedAt);
    }

    /** 保存直前に更新日時を刻む(application層がClockから渡す) */
    public Tournament touched(Instant now) {
        return new Tournament(id, name, gameType, competitionType, teamSize, totalRounds,
                currentRound, status, visibility, shareToken, resultInputEnabled, ownerSub, version,
                createdAt, now);
    }

    private Tournament withStatus(TournamentStatus newStatus, int newCurrentRound) {
        return new Tournament(id, name, gameType, competitionType, teamSize, totalRounds,
                newCurrentRound, newStatus, visibility, shareToken, resultInputEnabled, ownerSub,
                version, createdAt, updatedAt);
    }
}
