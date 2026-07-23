package com.swiss_stage.domain.model;

/**
 * 団体戦の順位表の1行(計算結果。保存しない。個人戦のStandingと対称)。
 * points / sos / sosos は精度問題を避けるため2倍整数(勝=2, 分=1)。
 */
public record TeamStanding(
        int rank,
        TeamId teamId,
        int wins,
        int losses,
        int draws,
        int points,
        int sos,
        int sosos,
        boolean hadBye) {

    public TeamStanding withRank(int newRank) {
        return new TeamStanding(newRank, teamId, wins, losses, draws, points, sos, sosos, hadBye);
    }
}
