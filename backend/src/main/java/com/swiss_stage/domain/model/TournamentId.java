package com.swiss_stage.domain.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.swiss_stage.domain.DomainException;

public record TournamentId(String value) {

    public TournamentId {
        if (value == null || value.isBlank()) {
            throw new DomainException("TournamentIdが空です");
        }
    }

    public static TournamentId generate() {
        return new TournamentId(UlidCreator.getUlid().toString());
    }
}
