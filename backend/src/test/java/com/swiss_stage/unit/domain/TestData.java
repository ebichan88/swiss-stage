package com.swiss_stage.unit.domain;

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

    static Participant participant(int seedOrder) {
        return participant(seedOrder, (String) null);
    }

    static Participant participant(int seedOrder, String organization) {
        return new Participant(
                new ParticipantId("P" + String.format("%03d", seedOrder)),
                "参加者" + seedOrder,
                organization,
                null,
                seedOrder,
                ParticipantStatus.ACTIVE);
    }

    static Participant participant(int seedOrder, Rank rank) {
        return new Participant(
                new ParticipantId("P" + String.format("%03d", seedOrder)),
                "参加者" + seedOrder,
                null,
                rank,
                seedOrder,
                ParticipantStatus.ACTIVE);
    }

    static List<Participant> participants(int count) {
        List<Participant> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(participant(i));
        }
        return list;
    }

    static Match match(int round, Participant p1, Participant p2, MatchResult result) {
        return Match.pairOf(round, 1, p1.id(), p2.id()).withResult(result);
    }

    static Match bye(int round, Participant p) {
        return Match.byeOf(round, 99, p.id());
    }
}
