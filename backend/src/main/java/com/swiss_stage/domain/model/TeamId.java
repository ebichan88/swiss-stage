package com.swiss_stage.domain.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.swiss_stage.domain.DomainException;

public record TeamId(String value) {

    public TeamId {
        if (value == null || value.isBlank()) {
            throw new DomainException("TeamIdが空です");
        }
    }

    public static TeamId generate() {
        return new TeamId(UlidCreator.getUlid().toString());
    }
}
