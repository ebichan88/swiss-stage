package com.swiss_stage.domain.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.swiss_stage.domain.DomainException;

public record TeamMatchId(String value) {

    public TeamMatchId {
        if (value == null || value.isBlank()) {
            throw new DomainException("TeamMatchIdが空です");
        }
    }

    public static TeamMatchId generate() {
        return new TeamMatchId(UlidCreator.getUlid().toString());
    }
}
