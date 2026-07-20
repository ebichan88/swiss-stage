package com.swiss_stage.domain.service;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Rank;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * グループ割当のドメインルール(.claude/01_development_docs/05_swiss_pairing_algorithm.md §2.4)。
 * 入力(グループ+参加者) → 出力(割当案/検証)の純粋関数。
 */
public class GroupAssignmentService {

    /**
     * 段級位による自動振り分け案。棋力の強い順(同棋力・未入力はエントリー順、未入力は末尾)に
     * ソートし、グループ定義順にできるだけ均等な人数で先頭から分割する(端数は先頭側へ)。
     */
    public Map<ParticipantId, GroupId> propose(List<Group> groups, List<Participant> participants) {
        if (groups.isEmpty()) {
            throw new DomainException("グループが定義されていません");
        }
        List<Participant> active = participants.stream()
                .filter(Participant::isActive)
                .sorted(byStrength())
                .toList();
        Map<ParticipantId, GroupId> assignment = new LinkedHashMap<>();
        int base = active.size() / groups.size();
        int remainder = active.size() % groups.size();
        int index = 0;
        for (int g = 0; g < groups.size(); g++) {
            int size = base + (g < remainder ? 1 : 0);
            for (int i = 0; i < size; i++) {
                assignment.put(active.get(index++).id(), groups.get(g).id());
            }
        }
        return assignment;
    }

    /**
     * 大会開始時の検証。全ACTIVE参加者の割当先が定義済みグループで、
     * 各グループのACTIVE参加者が2名以上であること。違反時はDomainException。
     */
    public void validateForStart(List<Group> groups, List<Participant> participants) {
        if (groups.isEmpty()) {
            throw new DomainException("グループが定義されていません");
        }
        Set<GroupId> groupIds = groups.stream().map(Group::id).collect(Collectors.toSet());
        List<Participant> active = participants.stream().filter(Participant::isActive).toList();
        long unassigned = active.stream()
                .filter(p -> !groupIds.contains(p.groupId()))
                .count();
        if (unassigned > 0) {
            throw new DomainException(
                    "グループ未割当の参加者が" + unassigned + "名います。全員を割り当ててから開始してください");
        }
        for (Group group : groups) {
            long count = active.stream().filter(p -> group.id().equals(p.groupId())).count();
            if (count < 2) {
                throw new DomainException(
                        "グループ「" + group.name() + "」の参加者が2名未満です。2名以上にしてから開始してください");
            }
        }
    }

    private static Comparator<Participant> byStrength() {
        return Comparator
                .comparing(Participant::rank, Rank.strongestFirst())
                .thenComparingInt(Participant::entryOrder);
    }
}
