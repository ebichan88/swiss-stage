package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import java.util.Map;

/** 対局DTO。player2 が null なら不戦勝(BYE) */
public record MatchDto(
        String id,
        int roundNumber,
        int tableNumber,
        ParticipantSummaryDto player1,
        ParticipantSummaryDto player2,
        MatchResult result,
        long version) {

    public static MatchDto from(Match m, Map<ParticipantId, Participant> participants) {
        return new MatchDto(
                m.id().value(),
                m.roundNumber(),
                m.tableNumber(),
                summaryOf(m.player1Id(), participants),
                m.player2Id() == null ? null : summaryOf(m.player2Id(), participants),
                m.result(),
                m.version());
    }

    private static ParticipantSummaryDto summaryOf(
            ParticipantId id, Map<ParticipantId, Participant> participants) {
        Participant p = participants.get(id);
        // 参加者が見つからない不整合データでもレスポンス全体を壊さない
        return p == null
                ? new ParticipantSummaryDto(id.value(), "(不明な参加者)", null)
                : ParticipantSummaryDto.from(p);
    }
}
