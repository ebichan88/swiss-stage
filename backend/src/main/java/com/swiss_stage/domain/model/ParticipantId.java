package com.swiss_stage.domain.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.swiss_stage.domain.DomainException;

public record ParticipantId(String value) {

    public ParticipantId {
        if (value == null || value.isBlank()) {
            throw new DomainException("ParticipantIdが空です");
        }
    }

    public static ParticipantId generate() {
        return new ParticipantId(UlidCreator.getUlid().toString());
    }
}
