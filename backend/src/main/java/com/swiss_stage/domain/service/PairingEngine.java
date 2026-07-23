package com.swiss_stage.domain.service;

import com.swiss_stage.domain.DomainException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * スイス方式ペアリングのバックトラック探索アルゴリズムの汎用コア(個人戦・団体戦で共通)。
 *
 * <p>エントラントの型(Participant/Team)やID型に依存しないよう、必要な属性(初回ラウンドの
 * 並び順・勝点・SOS・BYE経験・エントリー順・同一グループ回避判定)はすべて呼び出し側が
 * 関数として渡す。アルゴリズム本体(バックトラック・制約緩和の順序)は
 * {@code SwissPairingService} から移設したもので、挙動は変えていない。
 * 仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §2, §5.2
 */
final class PairingEngine {

    private static final int SEARCH_NODE_LIMIT = 200_000;

    /** 初回ラウンド: 呼び出し側が渡す順序(強い順など)でソートし、隣接ペアリングする */
    <E, ID> PairingOutcome<ID> pairFirstRound(
            List<E> active,
            Function<E, ID> idOf,
            Comparator<E> order,
            boolean randomize,
            long randomSeed) {
        List<E> ordered = new ArrayList<>(active);
        ordered.sort(order);
        if (randomize) {
            Collections.shuffle(ordered, new Random(randomSeed));
        }

        ID bye = null;
        if (ordered.size() % 2 != 0) {
            bye = idOf.apply(ordered.removeLast());
        }
        List<PairingOutcome.Pair<ID>> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < ordered.size(); i += 2) {
            pairs.add(new PairingOutcome.Pair<>(idOf.apply(ordered.get(i)), idOf.apply(ordered.get(i + 1))));
        }
        return new PairingOutcome<>(pairs, bye, Set.of());
    }

    /** 第2ラウンド以降: スコアグループ + バックトラック探索 */
    <E, ID> PairingOutcome<ID> pairLaterRound(
            List<E> active,
            Function<E, ID> idOf,
            Function<ID, String> rawId,
            ToIntFunction<E> pointsOf,
            ToIntFunction<E> sosOf,
            Predicate<E> hadByeOf,
            ToIntFunction<E> entryOrderOf,
            Set<PairKey<ID>> playedPairs,
            BiPredicate<E, E> sameAvoidGroup,
            boolean avoidGroupEnabled) {
        Set<PairingRelaxation> relaxations = EnumSet.noneOf(PairingRelaxation.class);
        ID bye = null;
        List<E> toPair = new ArrayList<>(active);
        if (active.size() % 2 != 0) {
            E byeEntrant = selectBye(active, pointsOf, sosOf, hadByeOf, entryOrderOf, relaxations);
            bye = idOf.apply(byeEntrant);
            toPair.remove(byeEntrant);
        }

        // 勝点降順 → SOS降順 → エントリー順。この順で先頭から相手を探す
        toPair.sort(Comparator
                .comparingInt(pointsOf).reversed()
                .thenComparing(Comparator.comparingInt(sosOf).reversed())
                .thenComparingInt(entryOrderOf));

        // 制約レベルを段階的に緩和して探索する
        List<PairingOutcome.Pair<ID>> pairs =
                search(toPair, idOf, rawId, pointsOf, playedPairs, true, avoidGroupEnabled, sameAvoidGroup);
        if (pairs == null && avoidGroupEnabled) {
            pairs = search(toPair, idOf, rawId, pointsOf, playedPairs, true, false, sameAvoidGroup);
            if (pairs != null) {
                relaxations.add(PairingRelaxation.SAME_ORGANIZATION);
            }
        }
        if (pairs == null) {
            pairs = search(toPair, idOf, rawId, pointsOf, playedPairs, false, false, sameAvoidGroup);
            if (pairs != null) {
                relaxations.add(PairingRelaxation.REMATCH);
            }
        }
        if (pairs == null) {
            throw new DomainException("組み合わせを生成できませんでした");
        }
        return new PairingOutcome<>(pairs, bye, Collections.unmodifiableSet(relaxations));
    }

    /**
     * BYE付与: BYE未経験 かつ 最下位スコアグループ かつ SOS最小。
     * 全員BYE経験済みの場合のみBYE重複を許し、緩和として記録する。
     */
    private <E> E selectBye(
            List<E> active,
            ToIntFunction<E> pointsOf,
            ToIntFunction<E> sosOf,
            Predicate<E> hadByeOf,
            ToIntFunction<E> entryOrderOf,
            Set<PairingRelaxation> relaxations) {
        Comparator<E> lowestFirst = Comparator
                .comparingInt(pointsOf)
                .thenComparingInt(sosOf)
                .thenComparing(Comparator.comparingInt(entryOrderOf).reversed());
        Optional<E> candidate = active.stream().filter(e -> !hadByeOf.test(e)).min(lowestFirst);
        if (candidate.isPresent()) {
            return candidate.get();
        }
        relaxations.add(PairingRelaxation.BYE_REPEAT);
        return active.stream().min(lowestFirst).orElseThrow();
    }

    /** バックトラック探索。解がなければnull */
    private <E, ID> List<PairingOutcome.Pair<ID>> search(
            List<E> ordered,
            Function<E, ID> idOf,
            Function<ID, String> rawId,
            ToIntFunction<E> pointsOf,
            Set<PairKey<ID>> playedPairs,
            boolean forbidRematch,
            boolean avoidSameGroup,
            BiPredicate<E, E> sameAvoidGroup) {
        var context = new SearchContext<>(
                idOf, rawId, pointsOf, playedPairs, forbidRematch, avoidSameGroup, sameAvoidGroup);
        List<PairingOutcome.Pair<ID>> result = new ArrayList<>();
        if (backtrack(ordered, new HashSet<>(), result, context)) {
            return result;
        }
        return null;
    }

    private <E, ID> boolean backtrack(
            List<E> ordered,
            Set<ID> paired,
            List<PairingOutcome.Pair<ID>> result,
            SearchContext<E, ID> ctx) {
        if (ctx.nodeCount++ > SEARCH_NODE_LIMIT) {
            return false;
        }
        E first = null;
        for (E e : ordered) {
            if (!paired.contains(ctx.idOf.apply(e))) {
                first = e;
                break;
            }
        }
        if (first == null) {
            return true; // 全員ペア済み
        }

        for (E candidate : candidatesFor(first, ordered, paired, ctx)) {
            ID firstId = ctx.idOf.apply(first);
            ID candidateId = ctx.idOf.apply(candidate);
            paired.add(firstId);
            paired.add(candidateId);
            result.add(new PairingOutcome.Pair<>(firstId, candidateId));
            if (backtrack(ordered, paired, result, ctx)) {
                return true;
            }
            result.removeLast();
            paired.remove(firstId);
            paired.remove(candidateId);
        }
        return false;
    }

    /**
     * 候補を優先度順に返す: 勝点差が小さい順(フロート最小化)→ 別グループ優先 → 元の並び順。
     */
    private <E, ID> List<E> candidatesFor(
            E first, List<E> ordered, Set<ID> paired, SearchContext<E, ID> ctx) {
        ID firstId = ctx.idOf.apply(first);
        int firstPoints = ctx.pointsOf.applyAsInt(first);
        List<E> candidates = new ArrayList<>();
        for (E e : ordered) {
            ID id = ctx.idOf.apply(e);
            if (paired.contains(id) || id.equals(firstId)) {
                continue;
            }
            if (ctx.forbidRematch && ctx.playedPairs.contains(PairKey.of(firstId, id, ctx.rawId))) {
                continue;
            }
            if (ctx.avoidSameGroup && ctx.sameAvoidGroup.test(first, e)) {
                continue;
            }
            candidates.add(e);
        }
        candidates.sort(Comparator.comparingInt(
                e -> Math.abs(ctx.pointsOf.applyAsInt(e) - firstPoints)));
        return candidates;
    }

    private static final class SearchContext<E, ID> {
        final Function<E, ID> idOf;
        final Function<ID, String> rawId;
        final ToIntFunction<E> pointsOf;
        final Set<PairKey<ID>> playedPairs;
        final boolean forbidRematch;
        final boolean avoidSameGroup;
        final BiPredicate<E, E> sameAvoidGroup;
        int nodeCount = 0;

        SearchContext(
                Function<E, ID> idOf,
                Function<ID, String> rawId,
                ToIntFunction<E> pointsOf,
                Set<PairKey<ID>> playedPairs,
                boolean forbidRematch,
                boolean avoidSameGroup,
                BiPredicate<E, E> sameAvoidGroup) {
            this.idOf = idOf;
            this.rawId = rawId;
            this.pointsOf = pointsOf;
            this.playedPairs = playedPairs;
            this.forbidRematch = forbidRematch;
            this.avoidSameGroup = avoidSameGroup;
            this.sameAvoidGroup = sameAvoidGroup;
        }
    }

    /** 順序に依存しない対戦ペアのキー(ID型に依存しないよう、生の文字列表現を呼び出し側から受け取る) */
    record PairKey<ID>(String low, String high) {
        static <ID> PairKey<ID> of(ID a, ID b, Function<ID, String> rawId) {
            String sa = rawId.apply(a);
            String sb = rawId.apply(b);
            return sa.compareTo(sb) <= 0 ? new PairKey<>(sa, sb) : new PairKey<>(sb, sa);
        }
    }

    /** マッチング結果(ID型に依存しない汎用形)。pairs の順序がそのまま卓番号(1始まり)になる */
    record PairingOutcome<ID>(List<Pair<ID>> pairs, ID byeId, Set<PairingRelaxation> relaxations) {
        record Pair<ID>(ID firstId, ID secondId) {}

        boolean hasBye() {
            return byeId != null;
        }
    }
}
