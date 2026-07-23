package com.swiss_stage.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * 順位計算アルゴリズムの汎用コア(個人戦・団体戦で共通)。エントラントの型(Participant/Team)や
 * ID型に依存しないよう、勝点は呼び出し側が2倍整数(勝=2,分=1,負=0)に変換した
 * {@link DecidedResult} の形で渡す。この点数スケールを用いることで、勝敗の内訳
 * (wins/losses/draws)も点数の値(2/1/0)だけから一意に導出できる。
 * アルゴリズム本体は{@code StandingCalculator}から移設したもので、挙動は変えていない。
 * 仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §3, §5.3, §5.5
 */
final class StandingEngine {

    <E, ID> List<StandingRow<ID>> calculate(
            List<E> entrants,
            Function<E, ID> idOf,
            ToIntFunction<E> entryOrderOf,
            List<DecidedResult<ID>> decided) {
        Map<ID, Integer> points = new HashMap<>();
        Map<ID, List<ID>> opponents = new HashMap<>();
        Map<ID, int[]> records = new HashMap<>(); // [wins, losses, draws]
        Map<ID, Boolean> hadBye = new HashMap<>();
        for (E e : entrants) {
            ID id = idOf.apply(e);
            points.put(id, 0);
            opponents.put(id, new ArrayList<>());
            records.put(id, new int[3]);
            hadBye.put(id, false);
        }

        for (DecidedResult<ID> result : decided) {
            accumulate(result, points, opponents, records, hadBye);
        }

        Map<ID, Integer> sos = new HashMap<>();
        for (E e : entrants) {
            ID id = idOf.apply(e);
            sos.put(id, opponents.get(id).stream().mapToInt(points::get).sum());
        }
        Map<ID, Integer> sosos = new HashMap<>();
        for (E e : entrants) {
            ID id = idOf.apply(e);
            sosos.put(id, opponents.get(id).stream().mapToInt(sos::get).sum());
        }

        Map<ID, Integer> entryOrders = new HashMap<>();
        for (E e : entrants) {
            entryOrders.put(idOf.apply(e), entryOrderOf.applyAsInt(e));
        }

        List<StandingRow<ID>> rows = new ArrayList<>();
        for (E e : entrants) {
            ID id = idOf.apply(e);
            int[] rec = records.get(id);
            rows.add(new StandingRow<>(0, id, rec[0], rec[1], rec[2],
                    points.get(id), sos.get(id), sosos.get(id), hadBye.get(id)));
        }

        // 勝点 → SOS → SOSOS で降順ソート(表示順の安定のため最後にエントリー順)
        Comparator<StandingRow<ID>> byKeys = Comparator
                .<StandingRow<ID>>comparingInt(StandingRow::points).reversed()
                .thenComparing(Comparator.<StandingRow<ID>>comparingInt(StandingRow::sos).reversed())
                .thenComparing(Comparator.<StandingRow<ID>>comparingInt(StandingRow::sosos).reversed());
        rows.sort(byKeys.thenComparing(r -> entryOrders.get(r.id())));

        applyHeadToHead(rows, decided, byKeys);
        return assignRanks(rows, decided, byKeys);
    }

    /** 同一キーで並んだ2名が直接対決済みなら勝者を上位に並べ替える */
    private <ID> void applyHeadToHead(
            List<StandingRow<ID>> rows, List<DecidedResult<ID>> decided, Comparator<StandingRow<ID>> byKeys) {
        for (int i = 0; i + 1 < rows.size(); i++) {
            StandingRow<ID> a = rows.get(i);
            StandingRow<ID> b = rows.get(i + 1);
            boolean pairTie = byKeys.compare(a, b) == 0
                    && (i == 0 || byKeys.compare(rows.get(i - 1), a) != 0)
                    && (i + 2 >= rows.size() || byKeys.compare(b, rows.get(i + 2)) != 0);
            if (!pairTie) {
                continue;
            }
            Optional<ID> winner = headToHeadWinner(a.id(), b.id(), decided);
            if (winner.isPresent() && winner.get().equals(b.id())) {
                rows.set(i, b);
                rows.set(i + 1, a);
            }
        }
    }

    private <ID> Optional<ID> headToHeadWinner(ID a, ID b, List<DecidedResult<ID>> decided) {
        return decided.stream()
                .filter(d -> !d.isBye() && d.involves(a) && d.involves(b))
                .findFirst()
                .flatMap(d -> {
                    if (d.pointsA() > d.pointsB()) {
                        return Optional.of(d.a());
                    }
                    if (d.pointsB() > d.pointsA()) {
                        return Optional.of(d.b());
                    }
                    return Optional.empty();
                });
    }

    /** 同順位あり(1,2,2,4形式)でrankを付与する。直接対決で並べ替えた2名は別順位になる */
    private <ID> List<StandingRow<ID>> assignRanks(
            List<StandingRow<ID>> sorted, List<DecidedResult<ID>> decided, Comparator<StandingRow<ID>> byKeys) {
        List<StandingRow<ID>> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            StandingRow<ID> current = sorted.get(i);
            int rank = i + 1;
            if (i > 0) {
                StandingRow<ID> prev = sorted.get(i - 1);
                boolean sameKeys = byKeys.compare(prev, current) == 0;
                boolean separatedByH2h = sameKeys
                        && headToHeadWinner(prev.id(), current.id(), decided).isPresent();
                if (sameKeys && !separatedByH2h) {
                    rank = ranked.get(i - 1).rank();
                }
            }
            ranked.add(current.withRank(rank));
        }
        return ranked;
    }

    private <ID> void accumulate(
            DecidedResult<ID> result,
            Map<ID, Integer> points,
            Map<ID, List<ID>> opponents,
            Map<ID, int[]> records,
            Map<ID, Boolean> hadBye) {
        ID a = result.a();
        if (!points.containsKey(a) || (result.b() != null && !points.containsKey(result.b()))) {
            return; // エントラント外の対局(不整合データ)は無視する
        }
        points.merge(a, result.pointsA(), Integer::sum);
        recordFor(a, result.pointsA(), records);
        if (result.isBye()) {
            hadBye.put(a, true);
            return;
        }
        ID b = result.b();
        points.merge(b, result.pointsB(), Integer::sum);
        recordFor(b, result.pointsB(), records);
        // BYE以外は相互に対戦相手として記録(SOS計算対象)
        opponents.get(a).add(b);
        opponents.get(b).add(a);
    }

    /** 点数(2倍値: 勝=2,分=1,負け・両者負け=0)から勝敗内訳を導出する */
    private static <ID> void recordFor(ID id, int points, Map<ID, int[]> records) {
        int[] rec = records.get(id);
        if (points == 2) {
            rec[0]++;
        } else if (points == 0) {
            rec[1]++;
        } else {
            rec[2]++;
        }
    }

    /** 決着済みの1対局分の点数(2倍値)。b=null はBYE(pointsBは無視される) */
    record DecidedResult<ID>(ID a, ID b, int pointsA, int pointsB) {
        boolean isBye() {
            return b == null;
        }

        boolean involves(ID id) {
            return a.equals(id) || (b != null && b.equals(id));
        }
    }

    /** 順位表の1行分の計算結果(ID型に依存しない汎用形) */
    record StandingRow<ID>(
            int rank, ID id, int wins, int losses, int draws, int points, int sos, int sosos,
            boolean hadBye) {
        StandingRow<ID> withRank(int newRank) {
            return new StandingRow<>(newRank, id, wins, losses, draws, points, sos, sosos, hadBye);
        }
    }
}
