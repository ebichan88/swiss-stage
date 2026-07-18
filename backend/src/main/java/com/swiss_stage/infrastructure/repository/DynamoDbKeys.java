package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.TournamentId;
import java.time.Instant;

/**
 * シングルテーブルのキー組み立て(.claude/01_development_docs/02_database_design.md §3)。
 * ユーザー入力を直接キーに混ぜない(IDはULID型を経由する)。
 */
final class DynamoDbKeys {

    static final String METADATA_SK = "METADATA";
    static final String ROUND_PREFIX = "ROUND#";
    static final String PARTICIPANT_PREFIX = "PARTICIPANT#";
    static final String GROUP_PREFIX = "GROUP#";

    private DynamoDbKeys() {}

    static String pk(TournamentId id) {
        return "TOURNAMENT#" + id.value();
    }

    static String participantSk(ParticipantId id) {
        return PARTICIPANT_PREFIX + id.value();
    }

    static String groupSk(GroupId id) {
        return GROUP_PREFIX + id.value();
    }

    /** ラウンド番号はゼロ埋め2桁(SKソートのため) */
    static String roundSk(int roundNumber) {
        return String.format("ROUND#%02d", roundNumber);
    }

    static String matchSk(int roundNumber, MatchId id) {
        return roundSk(roundNumber) + "#MATCH#" + id.value();
    }

    static String matchSkPrefix(int roundNumber) {
        return roundSk(roundNumber) + "#MATCH#";
    }

    static String gsi1Pk(String ownerSub) {
        return "USER#" + ownerSub;
    }

    static String gsi1Sk(Instant createdAt) {
        return "TOURNAMENT#" + createdAt.toString();
    }

    static String gsi2Pk(String shareToken) {
        return "SHARE#" + shareToken;
    }
}
