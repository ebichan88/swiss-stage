package com.swiss_stage.domain.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.swiss_stage.domain.DomainException;

public record TournamentMemberId(String value) {

  public TournamentMemberId {
    if (value == null || value.isBlank()) {
      throw new DomainException("TournamentMemberIdが空です");
    }
  }

  public static TournamentMemberId generate() {
    return new TournamentMemberId(UlidCreator.getUlid().toString());
  }
}
