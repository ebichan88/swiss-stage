package com.swiss_stage.domain.service;

import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Standing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 順位計算。入力(参加者+全対局)→ 出力(順位リスト)の純粋関数。
 *
 * <p>順位決定基準(上から順に適用): 勝点 → SOS → SOSOS → 直接対決 → 同順位。
 * SOSはBYEラウンドを除外する(除外方式を採用)。
 * 仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §3
 */
public final class StandingCalculator {

    public List<Standing> calculate(List<Participant> participants, List<Match> matches) {
        List<Match> decided = matches.stream().filter(m -> m.result().isDecided()).toList();

        Map<ParticipantId, Integer> points = new HashMap<>();
        Map<ParticipantId, List<ParticipantId>> opponents = new HashMap<>();
        Map<ParticipantId, int[]> records = new HashMap<>(); // [wins, losses, draws]
        Map<ParticipantId, Boolean> hadBye = new HashMap<>();
        for (Participant p : participants) {
            points.put(p.id(), 0);
            opponents.put(p.id(), new ArrayList<>());
            records.put(p.id(), new int[3]);
            hadBye.put(p.id(), false);
        }

        for (Match match : decided) {
            accumulate(match, points, opponents, records, hadBye);
        }

        Map<ParticipantId, Integer> sos = new HashMap<>();
        for (Participant p : participants) {
            sos.put(p.id(), opponents.get(p.id()).stream().mapToInt(points::get).sum());
        }
        Map<ParticipantId, Integer> sosos = new HashMap<>();
        for (Participant p : participants) {
            sosos.put(p.id(), opponents.get(p.id()).stream().mapToInt(sos::get).sum());
        }

        Map<ParticipantId, Integer> entryOrders = new HashMap<>();
        for (Participant p : participants) {
            entryOrders.put(p.id(), p.entryOrder());
        }

        List<Standing> standings = new ArrayList<>();
        for (Participant p : participants) {
            int[] rec = records.get(p.id());
            standings.add(new Standing(0, p.id(), rec[0], rec[1], rec[2],
                    points.get(p.id()), sos.get(p.id()), sosos.get(p.id()), hadBye.get(p.id())));
        }

        // 勝点 → SOS → SOSOS で降順ソート(表示順の安定のため最後にエントリー順)
        Comparator<Standing> byKeys = Comparator
                .comparingInt(Standing::points).reversed()
                .thenComparing(Comparator.comparingInt(Standing::sos).reversed())
                .thenComparing(Comparator.comparingInt(Standing::sosos).reversed());
        standings.sort(byKeys.thenComparing(s -> entryOrders.get(s.participantId())));

        applyHeadToHead(standings, decided, byKeys);
        return assignRanks(standings, decided, byKeys);
    }

    /** 同一キーで並んだ2名が直接対決済みなら勝者を上位に並べ替える */
    private void applyHeadToHead(List<Standing> standings, List<Match> decided, Comparator<Standing> byKeys) {
        for (int i = 0; i + 1 < standings.size(); i++) {
            Standing a = standings.get(i);
            Standing b = standings.get(i + 1);
            boolean pairTie = byKeys.compare(a, b) == 0
                    && (i == 0 || byKeys.compare(standings.get(i - 1), a) != 0)
                    && (i + 2 >= standings.size() || byKeys.compare(b, standings.get(i + 2)) != 0);
            if (!pairTie) {
                continue;
            }
            Optional<ParticipantId> winner = headToHeadWinner(a.participantId(), b.participantId(), decided);
            if (winner.isPresent() && winner.get().equals(b.participantId())) {
                standings.set(i, b);
                standings.set(i + 1, a);
            }
        }
    }

    private Optional<ParticipantId> headToHeadWinner(ParticipantId a, ParticipantId b, List<Match> decided) {
        return decided.stream()
                .filter(m -> !m.isBye() && m.involves(a) && m.involves(b))
                .findFirst()
                .flatMap(m -> switch (m.result()) {
                    case PLAYER1_WIN -> Optional.of(m.player1Id());
                    case PLAYER2_WIN -> Optional.of(m.player2Id());
                    default -> Optional.empty();
                });
    }

    /** 同順位あり(1,2,2,4形式)でrankを付与する。直接対決で並べ替えた2名は別順位になる */
    private List<Standing> assignRanks(List<Standing> sorted, List<Match> decided, Comparator<Standing> byKeys) {
        List<Standing> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Standing current = sorted.get(i);
            int rank = i + 1;
            if (i > 0) {
                Standing prev = sorted.get(i - 1);
                boolean sameKeys = byKeys.compare(prev, current) == 0;
                boolean separatedByH2h = sameKeys
                        && headToHeadWinner(prev.participantId(), current.participantId(), decided).isPresent();
                if (sameKeys && !separatedByH2h) {
                    rank = ranked.get(i - 1).rank();
                }
            }
            ranked.add(current.withRank(rank));
        }
        return ranked;
    }

    private void accumulate(
            Match match,
            Map<ParticipantId, Integer> points,
            Map<ParticipantId, List<ParticipantId>> opponents,
            Map<ParticipantId, int[]> records,
            Map<ParticipantId, Boolean> hadBye) {
        ParticipantId p1 = match.player1Id();
        if (!points.containsKey(p1) || (match.player2Id() != null && !points.containsKey(match.player2Id()))) {
            return; // 参加者リスト外の対局(不整合データ)は無視する
        }
        points.merge(p1, match.result().pointsForPlayer1(), Integer::sum);
        if (match.isBye()) {
            hadBye.put(p1, true);
            records.get(p1)[0]++; // BYEは1勝として扱う
            return;
        }
        ParticipantId p2 = match.player2Id();
        points.merge(p2, match.result().pointsForPlayer2(), Integer::sum);
        // BYE以外は相互に対戦相手として記録(SOS計算対象)
        opponents.get(p1).add(p2);
        opponents.get(p2).add(p1);
        switch (match.result()) {
            case PLAYER1_WIN -> {
                records.get(p1)[0]++;
                records.get(p2)[1]++;
            }
            case PLAYER2_WIN -> {
                records.get(p2)[0]++;
                records.get(p1)[1]++;
            }
            case DRAW -> {
                records.get(p1)[2]++;
                records.get(p2)[2]++;
            }
            case BOTH_LOSE -> {
                records.get(p1)[1]++;
                records.get(p2)[1]++;
            }
            default -> { }
        }
    }
}
