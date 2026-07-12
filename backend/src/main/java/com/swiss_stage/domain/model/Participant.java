package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;

/**
 * 参加者。organization / rank は任意(null許容)。
 */
public record Participant(
        ParticipantId id,
        String name,
        String organization,
        Rank rank,
        int seedOrder,
        ParticipantStatus status) {

    public Participant {
        if (name == null || name.isBlank()) {
            throw new DomainException("参加者の氏名は必須です");
        }
        if (seedOrder < 1) {
            throw new DomainException("シード順は1以上である必要があります");
        }
    }

    public static Participant create(String name, String organization, Rank rank, int seedOrder) {
        return new Participant(
                ParticipantId.generate(), name, organization, rank, seedOrder, ParticipantStatus.ACTIVE);
    }

    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
    }

    /** 途中棄権。以降のマッチング対象から外れる(過去の結果は順位計算に残る) */
    public Participant withdraw() {
        return new Participant(id, name, organization, rank, seedOrder, ParticipantStatus.WITHDRAWN);
    }

    public boolean hasSameOrganization(Participant other) {
        return organization != null
                && !organization.isBlank()
                && organization.equals(other.organization());
    }
}
