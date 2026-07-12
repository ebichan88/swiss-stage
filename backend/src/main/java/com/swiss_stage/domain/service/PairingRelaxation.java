package com.swiss_stage.domain.service;

/**
 * マッチング時に緩和された制約。空でない場合はUIに警告を表示する。
 */
public enum PairingRelaxation {
    /** 同一所属回避を緩和した */
    SAME_ORGANIZATION,
    /** 再戦禁止(絶対制約)を緩和した */
    REMATCH,
    /** BYE重複禁止(絶対制約)を緩和した */
    BYE_REPEAT
}
