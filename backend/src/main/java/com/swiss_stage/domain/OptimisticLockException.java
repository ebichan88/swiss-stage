package com.swiss_stage.domain;

/**
 * 楽観ロック競合(versionの不一致)。リポジトリ実装が送出し、application層で409に変換する。
 */
public class OptimisticLockException extends DomainException {

    public OptimisticLockException(String message) {
        super(message);
    }
}
