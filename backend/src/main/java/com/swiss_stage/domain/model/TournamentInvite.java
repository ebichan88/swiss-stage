package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;
import java.time.Duration;
import java.time.Instant;

/**
 * 大会の招待リンク。1大会につき有効な招待は常に1本(再発行は同じアイテムの上書き)。 {@code tournamentId} はトークンからの逆引き(GSI2)で大会を特定するために保持する
 * (14_tournament_collaboration.md §4.3・§4.4)。
 *
 * <p>{@code version} は同時承諾での枠超過を防ぐ楽観ロックに使う。発行・再発行はOWNERの単独操作のため
 * 競合を検出する必要がなく、直前に読み込んだ(または未発行なら存在しない)状態のversionをそのまま 引き継いで保存する(Tournamentの{@code
 * touched()}と同じ書き方)。
 */
public record TournamentInvite(
    TournamentId tournamentId,
    String token,
    Instant expiresAt,
    int maxUses,
    int usedCount,
    long version,
    Instant createdAt) {

  /** 発行から72時間固定(UIで期限を選ばせない。シンプルさ優先) */
  public static final Duration VALIDITY = Duration.ofHours(72);

  public TournamentInvite {
    if (tournamentId == null) {
      throw new DomainException("招待の大会IDは必須です");
    }
    if (token == null || token.isBlank()) {
      throw new DomainException("招待トークンは必須です");
    }
    if (maxUses < 1) {
      throw new DomainException("招待の人数枠は1以上である必要があります");
    }
    if (usedCount < 0) {
      throw new DomainException("招待の使用済み人数が不正です");
    }
    if (expiresAt == null || createdAt == null) {
      throw new DomainException("招待の発行日時・有効期限は必須です");
    }
  }

  /**
   * 新規発行・再発行。{@code previousVersion} には未発行ならnull、既存招待の再発行ならその招待の
   * versionを渡す(楽観ロックの条件を満たすため。呼び出し元はTournamentAccessSupport経由でOWNERを 確認済みであること前提)。
   */
  public static TournamentInvite issue(
      TournamentId tournamentId, String token, int maxUses, Instant now, Long previousVersion) {
    return new TournamentInvite(
        tournamentId,
        token,
        now.plus(VALIDITY),
        maxUses,
        0,
        previousVersion == null ? 0L : previousVersion,
        now);
  }

  /** 期限内かつ人数枠に余裕があるか */
  public boolean isAcceptable(Instant now) {
    return usedCount < maxUses && now.isBefore(expiresAt);
  }

  /** 承諾による使用済み人数の加算 */
  public TournamentInvite accepted() {
    return new TournamentInvite(
        tournamentId, token, expiresAt, maxUses, usedCount + 1, version, createdAt);
  }

  public int remainingUses() {
    return maxUses - usedCount;
  }
}
