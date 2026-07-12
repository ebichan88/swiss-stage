package com.swiss_stage.domain.model;

/**
 * 順位表の1行(計算結果。保存しない)。
 * points / sos / sosos は精度問題を避けるため2倍整数(勝=2, 分=1)。
 */
public record Standing(
        int rank,
        ParticipantId participantId,
        int wins,
        int losses,
        int draws,
        int points,
        int sos,
        int sosos,
        boolean hadBye) {

    public Standing withRank(int newRank) {
        return new Standing(newRank, participantId, wins, losses, draws, points, sos, sosos, hadBye);
    }
}
