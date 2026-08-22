package com.swiss_stage.application.service;

import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.ForbiddenException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import org.springframework.stereotype.Component;

/**
 * 大会の取得と認可(13_security_design.md §3、14_tournament_collaboration.md §4.1)。
 *
 * <p>404と403の使い分け: メンバーでない(OWNERでもMAINTAINERでもない)場合は他人の大会の存在を
 * 漏らさないため404。メンバーではあるが権限が足りない(MAINTAINERがOWNER専用操作を呼んだ)場合は
 * 403(「自分が所属している大会である」以上の情報は増えないため存在秘匿は保たれる)。
 */
@Component
public class TournamentAccessSupport {

  private final TournamentRepository tournamentRepository;
  private final TournamentMemberRepository memberRepository;

  public TournamentAccessSupport(
      TournamentRepository tournamentRepository, TournamentMemberRepository memberRepository) {
    this.tournamentRepository = tournamentRepository;
    this.memberRepository = memberRepository;
  }

  /** OWNER専用操作(大会設定・削除・共有トークン再発行・招待/メンバー管理)で使う */
  public Tournament loadOwner(TournamentId id, String sub) {
    Tournament tournament = loadMember(id, sub);
    if (!tournament.isOwnedBy(sub)) {
      throw new ForbiddenException();
    }
    return tournament;
  }

  /** OWNER・MAINTAINERのどちらでも許可する操作(それ以外の全ユースケース)で使う */
  public Tournament loadMember(TournamentId id, String sub) {
    Tournament tournament =
        tournamentRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TOURNAMENT_NOT_FOUND));
    if (tournament.isOwnedBy(sub) || memberRepository.findBySub(id, sub).isPresent()) {
      return tournament;
    }
    throw new NotFoundException(ErrorCode.TOURNAMENT_NOT_FOUND);
  }
}
