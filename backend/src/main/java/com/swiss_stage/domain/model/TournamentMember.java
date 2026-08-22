package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;
import java.time.Instant;

/**
 * 大会の共同管理者(MVPでは常にMAINTAINER。OWNERはMEMBERアイテムを持たない。ownerSubが正)。
 * displayNameは承諾時のGoogle表示名(個人情報)であり、OWNER向けレスポンス以外に出さない・ログに出さない (14_tournament_collaboration.md
 * §4.3)。
 */
public record TournamentMember(
    TournamentMemberId id, String sub, TournamentRole role, String displayName, Instant joinedAt) {

  public TournamentMember {
    if (sub == null || sub.isBlank()) {
      throw new DomainException("共同管理者のsubは必須です");
    }
    if (role == null) {
      throw new DomainException("共同管理者のroleは必須です");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new DomainException("共同管理者の表示名は必須です");
    }
    if (joinedAt == null) {
      throw new DomainException("共同管理者の参加日時は必須です");
    }
  }

  public static TournamentMember create(String sub, String displayName, Instant now) {
    return new TournamentMember(
        TournamentMemberId.generate(), sub, TournamentRole.MAINTAINER, displayName, now);
  }
}
