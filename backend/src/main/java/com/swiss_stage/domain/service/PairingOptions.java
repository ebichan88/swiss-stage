package com.swiss_stage.domain.service;

/**
 * マッチングのオプション。
 *
 * @param randomFirstRound 初回ラウンドをランダムペアリングにする(デフォルトはエントリー順)
 * @param avoidSameOrganization 同一所属同士の対戦を可能な限り避ける(デフォルトON)
 * @param randomSeed ランダムペアリング時のシード(再現性のため)
 */
public record PairingOptions(boolean randomFirstRound, boolean avoidSameOrganization, long randomSeed) {

    public static PairingOptions defaults() {
        return new PairingOptions(false, true, 0L);
    }
}
