package com.swiss_stage.domain.service;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.Standing;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * スイス方式マッチング(個人戦)。バックトラック探索の本体は{@link PairingEngine}に共通化し、
 * このクラスは Participant/Match をエンジンの汎用形に変換するアダプタに徹する
 * (団体戦は {@code TeamSwissPairingService} が同じ PairingEngine を使う)。
 *
 * <p>制約(仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §2):
 * <ul>
 *   <li>[絶対] 再戦禁止 / BYE重複禁止(解がない場合のみ緩和し、結果に記録する)</li>
 *   <li>[優先] 同一所属回避(緩和可)</li>
 * </ul>
 */
public final class SwissPairingService {

    private final StandingCalculator standingCalculator = new StandingCalculator();
    private final PairingEngine engine = new PairingEngine();

    /**
     * @param participants 全参加者(WITHDRAWNは自動的に除外される)
     * @param previousMatches 過去全ラウンドの対局(再戦判定・成績計算に使用)
     * @param roundNumber 生成するラウンド番号(1始まり)
     */
    public PairingResult pair(
            List<Participant> participants,
            List<Match> previousMatches,
            int roundNumber,
            PairingOptions options) {
        List<Participant> active = participants.stream().filter(Participant::isActive).toList();
        if (active.size() < 2) {
            throw new DomainException("マッチングには2名以上の参加者が必要です");
        }
        if (roundNumber == 1) {
            return pairFirstRound(active, options);
        }
        return pairLaterRound(participants, active, previousMatches, options);
    }

    // --- 初回ラウンド: 棋力の近い者同士を組む(棋力の強い順にソートして隣接ペアリング) ---

    private PairingResult pairFirstRound(List<Participant> active, PairingOptions options) {
        Comparator<Participant> order = Comparator
                .comparing(Participant::rank, Rank.strongestFirst())
                .thenComparingInt(Participant::entryOrder);
        PairingEngine.PairingOutcome<ParticipantId> outcome = engine.pairFirstRound(
                active, Participant::id, order, options.randomFirstRound(), options.randomSeed());
        return toPairingResult(outcome);
    }

    // --- 第2ラウンド以降: スコアグループ + バックトラック ---

    private PairingResult pairLaterRound(
            List<Participant> allParticipants,
            List<Participant> active,
            List<Match> previousMatches,
            PairingOptions options) {
        // 成績は棄権者を含む全参加者で計算する(棄権者との過去対局も勝点グループに反映するため)
        Map<ParticipantId, Standing> standings =
                standingCalculator.calculate(allParticipants, previousMatches).stream()
                        .collect(Collectors.toMap(Standing::participantId, Function.identity()));
        Set<PairingEngine.PairKey<ParticipantId>> playedPairs = collectPlayedPairs(previousMatches);

        PairingEngine.PairingOutcome<ParticipantId> outcome = engine.pairLaterRound(
                active,
                Participant::id,
                ParticipantId::value,
                p -> standings.get(p.id()).points(),
                p -> standings.get(p.id()).sos(),
                p -> standings.get(p.id()).hadBye(),
                Participant::entryOrder,
                playedPairs,
                Participant::hasSameOrganization,
                options.avoidSameOrganization());
        return toPairingResult(outcome);
    }

    private Set<PairingEngine.PairKey<ParticipantId>> collectPlayedPairs(List<Match> matches) {
        Set<PairingEngine.PairKey<ParticipantId>> keys = new HashSet<>();
        for (Match m : matches) {
            if (!m.isBye()) {
                keys.add(PairingEngine.PairKey.of(m.player1Id(), m.player2Id(), ParticipantId::value));
            }
        }
        return keys;
    }

    private static PairingResult toPairingResult(PairingEngine.PairingOutcome<ParticipantId> outcome) {
        List<PairingResult.Pair> pairs = outcome.pairs().stream()
                .map(p -> new PairingResult.Pair(p.firstId(), p.secondId()))
                .toList();
        return new PairingResult(pairs, outcome.byeId(), outcome.relaxations());
    }
}
