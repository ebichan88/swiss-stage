package com.swiss_stage.domain;

/**
 * ラウンドの二重生成。リポジトリ実装が条件付き書き込みの失敗として検出し、
 * application層で409(ROUND_ALREADY_EXISTS)に変換する。
 */
public class DuplicateRoundException extends DomainException {

    public DuplicateRoundException(String message) {
        super(message);
    }
}
