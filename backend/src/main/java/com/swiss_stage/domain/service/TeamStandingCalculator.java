package com.swiss_stage.domain.service;

import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamStanding;
import java.util.List;

/**
 * 順位計算(団体戦)。個人戦の{@link StandingCalculator}と同じ{@link StandingEngine}を使い、
 * Team/TeamMatch をエンジンの汎用形に変換するアダプタに徹する。入力(チーム+全対局)→
 * 出力(順位リスト)の純粋関数。
 *
 * <p>勝点・SOS・SOSOS・直接対決・エントリー順によるタイブレークは個人戦と全く同じ基準を
 * チーム単位に適用する。点数の元になる「対局結果」がボード集計値(TeamMatch#pointsFor)で
 * ある点のみが個人戦と異なる(05_swiss_pairing_algorithm.md §5.5)。
 */
public final class TeamStandingCalculator {

    private final StandingEngine engine = new StandingEngine();

    public List<TeamStanding> calculate(List<Team> teams, List<TeamMatch> matches) {
        List<StandingEngine.DecidedResult<TeamId>> decided = matches.stream()
                .filter(m -> m.isBye() || m.isFullyDecided())
                .map(m -> new StandingEngine.DecidedResult<>(
                        m.team1Id(),
                        m.isBye() ? null : m.team2Id(),
                        m.pointsFor(m.team1Id()),
                        m.isBye() ? 0 : m.pointsFor(m.team2Id())))
                .toList();

        return engine.calculate(teams, Team::id, Team::entryOrder, decided).stream()
                .map(row -> new TeamStanding(
                        row.rank(), row.id(), row.wins(), row.losses(), row.draws(), row.points(),
                        row.sos(), row.sosos(), row.hadBye()))
                .toList();
    }
}
