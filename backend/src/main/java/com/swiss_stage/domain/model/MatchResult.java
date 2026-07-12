package com.swiss_stage.domain.model;

/**
 * 対局結果。勝点は精度問題を避けるため2倍整数で扱う(勝=2, 分=1, 敗=0)。
 * 仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md
 */
public enum MatchResult {
    NONE,
    PLAYER1_WIN,
    PLAYER2_WIN,
    DRAW,
    BOTH_LOSE,
    BYE;

    /** player1が得る勝点(2倍値) */
    public int pointsForPlayer1() {
        return switch (this) {
            case PLAYER1_WIN, BYE -> 2;
            case DRAW -> 1;
            case PLAYER2_WIN, BOTH_LOSE, NONE -> 0;
        };
    }

    /** player2が得る勝点(2倍値) */
    public int pointsForPlayer2() {
        return switch (this) {
            case PLAYER2_WIN -> 2;
            case DRAW -> 1;
            case PLAYER1_WIN, BOTH_LOSE, BYE, NONE -> 0;
        };
    }

    public boolean isDecided() {
        return this != NONE;
    }
}
