package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;

/**
 * 棋力帯グループ(クラス分け)。マッチング・順位計算はグループごとに独立して行う。
 * (仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §2.4)
 */
public record Group(GroupId id, String name) {

    public static final int NAME_MAX_LENGTH = 50;

    public Group {
        if (name == null || name.isBlank()) {
            throw new DomainException("グループ名は必須です");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new DomainException("グループ名は" + NAME_MAX_LENGTH + "文字以内で入力してください");
        }
    }

    public static Group create(String name) {
        return new Group(GroupId.generate(), name);
    }

    public Group rename(String newName) {
        return new Group(id, newName);
    }
}
