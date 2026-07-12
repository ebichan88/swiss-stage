package com.swiss_stage.domain;

/**
 * ドメインルール違反を表す例外。
 * application層でAppException(HTTPステータス付き)に変換される。
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
