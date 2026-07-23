package com.swiss_stage.domain.service;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamStanding;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * スイス方式マッチング(団体戦)。個人戦の{@link SwissPairingService}と同じ
 * {@link PairingEngine} を使い、Team/TeamMatch をエンジンの汎用形に変換するアダプタに徹する。
 *
 * <p>個人戦との差分(仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §5.2):
 * <ul>
 *   <li>初回ラウンドのシードなし。エントリー順のみで隣接ペアリングする</li>
 *   <li>チームには「所属」の概念がないため、同一所属回避は適用しない</li>
 *   <li>再戦禁止・BYE重複禁止・バックトラックによる制約緩和は個人戦と同一のルール</li>
 * </ul>
 */
public final class TeamSwissPairingService {

    private final TeamStandingCalculator standingCalculator = new TeamStandingCalculator();
    private final PairingEngine engine = new PairingEngine();

    /**
     * @param teams 全チーム(WITHDRAWNは自動的に除外される)
     * @param previousMatches 過去全ラウンドの対局(再戦判定・成績計算に使用)
     * @param roundNumber 生成するラウンド番号(1始まり)
     */
    public PairingResult pair(List<Team> teams, List<TeamMatch> previousMatches, int roundNumber) {
        List<Team> active = teams.stream().filter(Team::isActive).toList();
        if (active.size() < 2) {
            throw new DomainException("マッチングには2チーム以上が必要です");
        }
        if (roundNumber == 1) {
            return pairFirstRound(active);
        }
        return pairLaterRound(teams, active, previousMatches);
    }

    // --- 初回ラウンド: シードなし。エントリー順のみで隣接ペアリング ---

    private PairingResult pairFirstRound(List<Team> active) {
        Comparator<Team> order = Comparator.comparingInt(Team::entryOrder);
        PairingEngine.PairingOutcome<TeamId> outcome =
                engine.pairFirstRound(active, Team::id, order, false, 0L);
        return toPairingResult(outcome);
    }

    // --- 第2ラウンド以降: スコアグループ + バックトラック(同一所属回避なし) ---

    private PairingResult pairLaterRound(
            List<Team> allTeams, List<Team> active, List<TeamMatch> previousMatches) {
        Map<TeamId, TeamStanding> standings =
                standingCalculator.calculate(allTeams, previousMatches).stream()
                        .collect(Collectors.toMap(TeamStanding::teamId, Function.identity()));
        Set<PairingEngine.PairKey<TeamId>> playedPairs = collectPlayedPairs(previousMatches);

        PairingEngine.PairingOutcome<TeamId> outcome = engine.pairLaterRound(
                active,
                Team::id,
                TeamId::value,
                t -> standings.get(t.id()).points(),
                t -> standings.get(t.id()).sos(),
                t -> standings.get(t.id()).hadBye(),
                Team::entryOrder,
                playedPairs,
                (a, b) -> false, // チームに所属の概念はないため同一所属回避は適用しない
                false);
        return toPairingResult(outcome);
    }

    private Set<PairingEngine.PairKey<TeamId>> collectPlayedPairs(List<TeamMatch> matches) {
        Set<PairingEngine.PairKey<TeamId>> keys = new HashSet<>();
        for (TeamMatch m : matches) {
            if (!m.isBye()) {
                keys.add(PairingEngine.PairKey.of(m.team1Id(), m.team2Id(), TeamId::value));
            }
        }
        return keys;
    }

    private static PairingResult toPairingResult(PairingEngine.PairingOutcome<TeamId> outcome) {
        List<PairingResult.Pair> pairs = outcome.pairs().stream()
                .map(p -> new PairingResult.Pair(p.firstId(), p.secondId()))
                .toList();
        return new PairingResult(pairs, outcome.byeId(), outcome.relaxations());
    }

    /** 団体戦のマッチング結果(ID型はTeamId固定。個人戦のPairingResultとは別レコード) */
    public record PairingResult(
            List<Pair> pairs, TeamId byeId, Set<PairingRelaxation> relaxations) {
        public record Pair(TeamId firstId, TeamId secondId) {}

        public boolean hasBye() {
            return byeId != null;
        }
    }
}
