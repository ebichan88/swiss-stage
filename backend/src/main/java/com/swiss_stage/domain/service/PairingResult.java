package com.swiss_stage.domain.service;

import com.swiss_stage.domain.model.ParticipantId;
import java.util.List;
import java.util.Set;

/**
 * マッチング結果。pairs の順序がそのまま卓番号(1始まり)になる。
 * Matchエンティティへの変換はapplication層で行う。
 */
public record PairingResult(
        List<Pair> pairs,
        ParticipantId byeParticipantId,
        Set<PairingRelaxation> relaxations) {

    public record Pair(ParticipantId player1Id, ParticipantId player2Id) {}

    public boolean hasBye() {
        return byeParticipantId != null;
    }
}
