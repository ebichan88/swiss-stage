package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;
import java.util.ArrayList;
import java.util.List;

/**
 * 団体戦のチーム(Participantに相当するマッチング単位)。
 * groupId は必須。個人戦と同じGroup機能をチーム単位で流用する(05_swiss_pairing_algorithm.md §5.2)。
 * 必須ポジション(1..teamSize)の過不足・補欠人数上限の検証はteamSize(Tournament側の属性)を
 * 要するため、Team単体では行わない(application/domain層の大会開始前検証で行う)。
 */
public record Team(
        TeamId id,
        String name,
        int entryOrder,
        ParticipantStatus status,
        GroupId groupId,
        List<TeamMember> members) {

    public static final int NAME_MAX_LENGTH = 50;

    public Team {
        if (name == null || name.isBlank()) {
            throw new DomainException("チーム名は必須です");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new DomainException("チーム名は" + NAME_MAX_LENGTH + "文字以内で入力してください");
        }
        if (entryOrder < 1) {
            throw new DomainException("エントリー順は1以上である必要があります");
        }
        if (groupId == null) {
            throw new DomainException("チームの帰属グループは必須です");
        }
        members = members == null ? List.of() : List.copyOf(members);
        long distinctPositions = members.stream()
                .map(TeamMember::boardPosition)
                .filter(p -> p != null)
                .distinct()
                .count();
        long positionedCount = members.stream().filter(m -> m.boardPosition() != null).count();
        if (distinctPositions != positionedCount) {
            throw new DomainException("同一チーム内でボード位置が重複しています");
        }
    }

    public static Team create(String name, int entryOrder, GroupId groupId) {
        return new Team(TeamId.generate(), name, entryOrder, ParticipantStatus.ACTIVE, groupId, List.of());
    }

    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
    }

    /** 途中棄権。以降のマッチング対象から外れる(過去の結果は順位計算に残る) */
    public Team withdraw() {
        return new Team(id, name, entryOrder, ParticipantStatus.WITHDRAWN, groupId, members);
    }

    /** グループ割当の変更 */
    public Team withGroup(GroupId newGroupId) {
        return new Team(id, name, entryOrder, status, newGroupId, members);
    }

    public Team rename(String newName) {
        return new Team(id, newName, entryOrder, status, groupId, members);
    }

    public Team withMember(TeamMember member) {
        List<TeamMember> updated = new ArrayList<>(members);
        updated.add(member);
        return new Team(id, name, entryOrder, status, groupId, updated);
    }

    public Team withoutMember(TeamMemberId memberId) {
        return new Team(id, name, entryOrder, status, groupId,
                members.stream().filter(m -> !m.id().equals(memberId)).toList());
    }

    public Team withReplacedMember(TeamMemberId memberId, TeamMember replacement) {
        return new Team(id, name, entryOrder, status, groupId,
                members.stream().map(m -> m.id().equals(memberId) ? replacement : m).toList());
    }

    public long reserveCount() {
        return members.stream().filter(TeamMember::isReserve).count();
    }
}
