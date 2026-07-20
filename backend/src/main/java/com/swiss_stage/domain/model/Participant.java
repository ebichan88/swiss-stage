package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;

/**
 * 参加者。organization / rank は任意(null許容)。
 * groupId は必須。参加者は常にいずれかのグループに帰属する(05_swiss_pairing_algorithm.md §2.4)。
 */
public record Participant(
        ParticipantId id,
        String name,
        String organization,
        Rank rank,
        int entryOrder,
        ParticipantStatus status,
        GroupId groupId) {

    public Participant {
        if (name == null || name.isBlank()) {
            throw new DomainException("参加者の氏名は必須です");
        }
        if (entryOrder < 1) {
            throw new DomainException("エントリー順は1以上である必要があります");
        }
        if (groupId == null) {
            throw new DomainException("参加者の帰属グループは必須です");
        }
    }

    public static Participant create(
            String name, String organization, Rank rank, int entryOrder, GroupId groupId) {
        return new Participant(
                ParticipantId.generate(), name, organization, rank, entryOrder,
                ParticipantStatus.ACTIVE, groupId);
    }

    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
    }

    /** 途中棄権。以降のマッチング対象から外れる(過去の結果は順位計算に残る) */
    public Participant withdraw() {
        return new Participant(id, name, organization, rank, entryOrder, ParticipantStatus.WITHDRAWN, groupId);
    }

    /** グループ割当の変更 */
    public Participant withGroup(GroupId newGroupId) {
        return new Participant(id, name, organization, rank, entryOrder, status, newGroupId);
    }

    public boolean hasSameOrganization(Participant other) {
        return organization != null
                && !organization.isBlank()
                && organization.equals(other.organization());
    }
}
