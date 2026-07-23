package com.swiss_stage.domain.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.swiss_stage.domain.DomainException;

public record TeamMemberId(String value) {

    public TeamMemberId {
        if (value == null || value.isBlank()) {
            throw new DomainException("TeamMemberIdが空です");
        }
    }

    public static TeamMemberId generate() {
        return new TeamMemberId(UlidCreator.getUlid().toString());
    }
}
