package com.swiss_stage.domain.service;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Standing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * スイス方式マッチング。
 *
 * <p>制約(仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §2):
 * <ul>
 *   <li>[絶対] 再戦禁止 / BYE重複禁止(解がない場合のみ緩和し、結果に記録する)</li>
 *   <li>[優先] 同一所属回避(緩和可)</li>
 * </ul>
 */
public final class SwissPairingService {

    /** バックトラック探索の展開ノード数上限(超えたら制約を緩和して再探索) */
    private static final int SEARCH_NODE_LIMIT = 200_000;

    private final StandingCalculator standingCalculator = new StandingCalculator();

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

    // --- 初回ラウンド: シード順(または乱数)で上位半分と下位半分を対応させる ---

    private PairingResult pairFirstRound(List<Participant> active, PairingOptions options) {
        List<Participant> ordered = new ArrayList<>(active);
        ordered.sort(Comparator.comparingInt(Participant::seedOrder));
        if (options.randomFirstRound()) {
            Collections.shuffle(ordered, new Random(options.randomSeed()));
        }

        ParticipantId bye = null;
        if (ordered.size() % 2 != 0) {
            bye = ordered.removeLast().id();
        }
        int half = ordered.size() / 2;
        List<PairingResult.Pair> pairs = new ArrayList<>();
        for (int i = 0; i < half; i++) {
            pairs.add(new PairingResult.Pair(ordered.get(i).id(), ordered.get(i + half).id()));
        }
        return new PairingResult(pairs, bye, Set.of());
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
        Set<PairKey> playedPairs = collectPlayedPairs(previousMatches);

        Set<PairingRelaxation> relaxations = EnumSet.noneOf(PairingRelaxation.class);
        ParticipantId bye = null;
        List<Participant> toPair = new ArrayList<>(active);
        if (active.size() % 2 != 0) {
            bye = selectBye(active, standings, relaxations);
            ParticipantId byeId = bye;
            toPair.removeIf(p -> p.id().equals(byeId));
        }

        // 勝点降順 → SOS降順 → シード順。この順で先頭から相手を探す
        toPair.sort(Comparator
                .comparingInt((Participant p) -> standings.get(p.id()).points()).reversed()
                .thenComparing(Comparator.comparingInt(
                        (Participant p) -> standings.get(p.id()).sos()).reversed())
                .thenComparing(Comparator.comparingInt(Participant::seedOrder)));

        // 制約レベルを段階的に緩和して探索する
        boolean avoidOrg = options.avoidSameOrganization();
        List<PairingResult.Pair> pairs = search(toPair, standings, playedPairs, true, avoidOrg);
        if (pairs == null && avoidOrg) {
            pairs = search(toPair, standings, playedPairs, true, false);
            if (pairs != null) {
                relaxations.add(PairingRelaxation.SAME_ORGANIZATION);
            }
        }
        if (pairs == null) {
            pairs = search(toPair, standings, playedPairs, false, false);
            if (pairs != null) {
                relaxations.add(PairingRelaxation.REMATCH);
            }
        }
        if (pairs == null) {
            throw new DomainException("組み合わせを生成できませんでした");
        }
        return new PairingResult(pairs, bye, Collections.unmodifiableSet(relaxations));
    }

    /**
     * BYE付与: BYE未経験 かつ 最下位スコアグループ かつ SOS最小。
     * 全員BYE経験済みの場合のみBYE重複を許し、緩和として記録する。
     */
    private ParticipantId selectBye(
            List<Participant> active,
            Map<ParticipantId, Standing> standings,
            Set<PairingRelaxation> relaxations) {
        Comparator<Participant> lowestFirst = Comparator
                .comparingInt((Participant p) -> standings.get(p.id()).points())
                .thenComparingInt(p -> standings.get(p.id()).sos())
                .thenComparing(Comparator.comparingInt(Participant::seedOrder).reversed());
        Optional<Participant> candidate = active.stream()
                .filter(p -> !standings.get(p.id()).hadBye())
                .min(lowestFirst);
        if (candidate.isPresent()) {
            return candidate.get().id();
        }
        relaxations.add(PairingRelaxation.BYE_REPEAT);
        return active.stream().min(lowestFirst).orElseThrow().id();
    }

    private Set<PairKey> collectPlayedPairs(List<Match> matches) {
        Set<PairKey> keys = new HashSet<>();
        for (Match m : matches) {
            if (!m.isBye()) {
                keys.add(PairKey.of(m.player1Id(), m.player2Id()));
            }
        }
        return keys;
    }

    /** バックトラック探索。解がなければnull */
    private List<PairingResult.Pair> search(
            List<Participant> ordered,
            Map<ParticipantId, Standing> standings,
            Set<PairKey> playedPairs,
            boolean forbidRematch,
            boolean avoidSameOrg) {
        var context = new SearchContext(standings, playedPairs, forbidRematch, avoidSameOrg);
        List<PairingResult.Pair> result = new ArrayList<>();
        if (backtrack(ordered, new HashSet<>(), result, context)) {
            return result;
        }
        return null;
    }

    private boolean backtrack(
            List<Participant> ordered,
            Set<ParticipantId> paired,
            List<PairingResult.Pair> result,
            SearchContext ctx) {
        if (ctx.nodeCount++ > SEARCH_NODE_LIMIT) {
            return false;
        }
        Participant first = null;
        for (Participant p : ordered) {
            if (!paired.contains(p.id())) {
                first = p;
                break;
            }
        }
        if (first == null) {
            return true; // 全員ペア済み
        }

        for (Participant candidate : candidatesFor(first, ordered, paired, ctx)) {
            paired.add(first.id());
            paired.add(candidate.id());
            result.add(new PairingResult.Pair(first.id(), candidate.id()));
            if (backtrack(ordered, paired, result, ctx)) {
                return true;
            }
            result.removeLast();
            paired.remove(first.id());
            paired.remove(candidate.id());
        }
        return false;
    }

    /**
     * 候補を優先度順に返す: 勝点差が小さい順(フロート最小化)→ 別所属優先 → 元の並び順。
     */
    private List<Participant> candidatesFor(
            Participant first, List<Participant> ordered, Set<ParticipantId> paired, SearchContext ctx) {
        int firstPoints = ctx.standings.get(first.id()).points();
        List<Participant> candidates = new ArrayList<>();
        for (Participant p : ordered) {
            if (paired.contains(p.id()) || p.id().equals(first.id())) {
                continue;
            }
            if (ctx.forbidRematch && ctx.playedPairs.contains(PairKey.of(first.id(), p.id()))) {
                continue;
            }
            if (ctx.avoidSameOrg && first.hasSameOrganization(p)) {
                continue;
            }
            candidates.add(p);
        }
        candidates.sort(Comparator.comparingInt(
                p -> Math.abs(ctx.standings.get(p.id()).points() - firstPoints)));
        return candidates;
    }

    private static final class SearchContext {
        final Map<ParticipantId, Standing> standings;
        final Set<PairKey> playedPairs;
        final boolean forbidRematch;
        final boolean avoidSameOrg;
        int nodeCount = 0;

        SearchContext(
                Map<ParticipantId, Standing> standings,
                Set<PairKey> playedPairs,
                boolean forbidRematch,
                boolean avoidSameOrg) {
            this.standings = standings;
            this.playedPairs = playedPairs;
            this.forbidRematch = forbidRematch;
            this.avoidSameOrg = avoidSameOrg;
        }
    }

    /** 順序に依存しない対戦ペアのキー */
    private record PairKey(String low, String high) {
        static PairKey of(ParticipantId a, ParticipantId b) {
            return a.value().compareTo(b.value()) <= 0
                    ? new PairKey(a.value(), b.value())
                    : new PairKey(b.value(), a.value());
        }
    }
}
