package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import java.util.Map;

/**
 * 対局DTO。player2 が null なら不戦勝(BYE)。
 * group は帰属グループ(必須)。卓表示は「A-1」形式に使う。
 */
public record MatchDto(
        String id,
        int roundNumber,
        int tableNumber,
        GroupDto group,
        ParticipantSummaryDto player1,
        ParticipantSummaryDto player2,
        MatchResult result,
        long version) {

    public static MatchDto from(
            Match m,
            Map<ParticipantId, Participant> participants,
            Map<GroupId, Group> groups) {
        Group group = groups.get(m.groupId());
        return new MatchDto(
                m.id().value(),
                m.roundNumber(),
                m.tableNumber(),
                // グループが見つからない不整合データでもレスポンス全体を壊さない
                group == null
                        ? new GroupDto(m.groupId().value(), "(不明なグループ)")
                        : GroupDto.from(group),
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
                ? new ParticipantSummaryDto(id.value(), "(不明な参加者)", null, null, 0)
                : ParticipantSummaryDto.from(p);
    }
}
