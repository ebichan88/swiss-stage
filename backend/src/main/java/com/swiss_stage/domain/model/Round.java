package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;

/**
 * ラウンドの状態。PAIRING → PLAYING → CONFIRMED の順にのみ遷移できる。
 */
public record Round(int roundNumber, RoundStatus status) {

    public Round {
        if (roundNumber < 1) {
            throw new DomainException("ラウンド番号は1以上である必要があります");
        }
    }

    public static Round pairing(int roundNumber) {
        return new Round(roundNumber, RoundStatus.PAIRING);
    }

    public Round startPlaying() {
        if (status != RoundStatus.PAIRING) {
            throw new DomainException("組み合わせ中のラウンドのみ対局開始できます");
        }
        return new Round(roundNumber, RoundStatus.PLAYING);
    }

    public Round confirm() {
        if (status != RoundStatus.PLAYING) {
            throw new DomainException("対局中のラウンドのみ確定できます");
        }
        return new Round(roundNumber, RoundStatus.CONFIRMED);
    }

    public boolean isConfirmed() {
        return status == RoundStatus.CONFIRMED;
    }
}
