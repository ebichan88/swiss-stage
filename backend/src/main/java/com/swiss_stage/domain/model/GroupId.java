package com.swiss_stage.domain.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.swiss_stage.domain.DomainException;

public record GroupId(String value) {

    public GroupId {
        if (value == null || value.isBlank()) {
            throw new DomainException("GroupIdが空です");
        }
    }

    public static GroupId generate() {
        // グループの表示順・自動振り分けの割当順はULID昇順(=作成順)が正。
        // 同一ミリ秒内の連続作成でも順序が崩れないよう単調増加ULIDを使う
        return new GroupId(UlidCreator.getMonotonicUlid().toString());
    }
}
