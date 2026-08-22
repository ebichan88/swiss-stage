package com.swiss_stage.application.service;

import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.function.IntSupplier;
import org.springframework.stereotype.Component;

/**
 * 参加者・チームのentryOrderを大会単位のカウンタで採番する(個人戦・団体戦で共有。 14_tournament_collaboration.md §4.5)。競合(同時採番)は
 * {@link com.swiss_stage.domain.OptimisticLockException} をそのまま呼び出し元へ伝播させ、 {@code
 * GlobalExceptionHandler} が409に変換する。招待承諾の採番とは異なり、サーバー側でのリトライは行わない (利用者に画面更新と再試行を促す)。
 */
@Component
public class TournamentEntryOrderAllocator {

  private final TournamentRepository tournamentRepository;
  private final Clock clock;

  public TournamentEntryOrderAllocator(TournamentRepository tournamentRepository, Clock clock) {
    this.tournamentRepository = tournamentRepository;
    this.clock = clock;
  }

  /**
   * {@code count} 個のentryOrderをまとめて確保し、確保した範囲の先頭値を返す。呼び出し側は [先頭値, 先頭値+count)
   * を順に使う。カウンタが未初期化(null)の場合は {@code initialValue} で初期値を算出する (既存参加者・チームの最大entryOrder+1、1件もなければ1)。
   */
  public int allocate(Tournament tournament, int count, IntSupplier initialValue) {
    int start =
        tournament.nextEntryOrder() != null ? tournament.nextEntryOrder() : initialValue.getAsInt();
    tournamentRepository.save(
        tournament.withNextEntryOrder(start + count).touched(Instant.now(clock)));
    return start;
  }
}
