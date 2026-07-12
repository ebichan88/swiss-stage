package com.swiss_stage.domain.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.swiss_stage.domain.DomainException;

public record MatchId(String value) {

    public MatchId {
        if (value == null || value.isBlank()) {
            throw new DomainException("MatchIdが空です");
        }
    }

    public static MatchId generate() {
        return new MatchId(UlidCreator.getUlid().toString());
    }
}
