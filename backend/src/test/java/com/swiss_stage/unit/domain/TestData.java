package com.swiss_stage.unit.domain;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Rank;
import java.util.ArrayList;
import java.util.List;

/** ドメインテスト用のデータビルダー(実在の個人名は使わない) */
final class TestData {

    private TestData() {}

    /** groupId必須のため、グループを区別しないテストはこの単一グループに全員帰属させる */
    static final GroupId GROUP_ID = new GroupId("01TESTGROUP000000000000000");

    static Participant participant(int entryOrder) {
        return participant(entryOrder, (String) null);
    }

    static Participant participant(int entryOrder, String organization) {
        return new Participant(
                new ParticipantId("P" + String.format("%03d", entryOrder)),
                "参加者" + entryOrder,
                organization,
                null,
                entryOrder,
                ParticipantStatus.ACTIVE,
                GROUP_ID);
    }

    static Participant participant(int entryOrder, Rank rank) {
        return new Participant(
                new ParticipantId("P" + String.format("%03d", entryOrder)),
                "参加者" + entryOrder,
                null,
                rank,
                entryOrder,
                ParticipantStatus.ACTIVE,
                GROUP_ID);
    }

    static List<Participant> participants(int count) {
        List<Participant> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(participant(i));
        }
        return list;
    }

    static Match match(int round, Participant p1, Participant p2, MatchResult result) {
        return Match.pairOf(round, 1, p1.id(), p2.id(), GROUP_ID).withResult(result);
    }

    static Match bye(int round, Participant p) {
        return Match.byeOf(round, 99, p.id(), GROUP_ID);
    }
}
