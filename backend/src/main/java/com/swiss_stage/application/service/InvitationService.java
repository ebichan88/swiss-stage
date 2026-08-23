package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.AcceptInvitationResultDto;
import com.swiss_stage.application.dto.InvitationPreviewDto;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.ForbiddenException;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentInvite;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.repository.TournamentInviteRepository;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 招待のプレビュー・承諾(認証済みユーザーなら誰でも呼べる。14_tournament_collaboration.md §4.4)。
 * 期限切れ・人数枠切れ・失効済み・不正トークンはすべて{@code INVALID_INVITE_TOKEN}の403に寄せ、
 * 理由を出し分けない(総当たりで有効なトークンの存在を推測させないため)。
 */
@Service
public class InvitationService {

  /** 同時承諾での競合時にサーバー内部で再試行する上限回数(通常はミリ秒単位の競合ウィンドウでしか起こらない) */
  private static final int MAX_ACCEPT_ATTEMPTS = 5;

  /** URL-safe Base64(ShareTokens.generate は43文字)。形式不正はキー組み立てに使わない */
  private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{32,64}");

  private final TournamentInviteRepository inviteRepository;
  private final TournamentRepository tournamentRepository;
  private final TournamentMemberRepository memberRepository;
  private final Clock clock;

  public InvitationService(
      TournamentInviteRepository inviteRepository,
      TournamentRepository tournamentRepository,
      TournamentMemberRepository memberRepository,
      Clock clock) {
    this.inviteRepository = inviteRepository;
    this.tournamentRepository = tournamentRepository;
    this.memberRepository = memberRepository;
    this.clock = clock;
  }

  public InvitationPreviewDto preview(String token, String sub) {
    TournamentInvite invite = loadInvite(token);
    Tournament tournament = loadTournament(invite);
    boolean alreadyMember = isMember(tournament, sub);
    if (!alreadyMember && !invite.isAcceptable(Instant.now(clock))) {
      throw new ForbiddenException(ErrorCode.INVALID_INVITE_TOKEN);
    }
    return new InvitationPreviewDto(
        tournament.id().value(),
        tournament.name(),
        tournament.gameType(),
        tournament.competitionType(),
        tournament.eventDate() == null ? null : tournament.eventDate().toString(),
        invite.expiresAt().toString(),
        alreadyMember);
  }

  public AcceptInvitationResultDto accept(String token, String sub, String displayName) {
    for (int attempt = 0; attempt < MAX_ACCEPT_ATTEMPTS; attempt++) {
      TournamentInvite invite = loadInvite(token);
      Tournament tournament = loadTournament(invite);
      // 既にOWNER本人・既存MAINTAINERなら冪等に成功させる(招待の有効性を問わない)
      if (isMember(tournament, sub)) {
        return new AcceptInvitationResultDto(tournament.id().value());
      }
      if (!invite.isAcceptable(Instant.now(clock))) {
        throw new ForbiddenException(ErrorCode.INVALID_INVITE_TOKEN);
      }
      TournamentMember member = TournamentMember.create(sub, displayName, Instant.now(clock));
      boolean accepted =
          inviteRepository.acceptWithMember(invite.accepted(), member, tournament.createdAt());
      if (accepted) {
        return new AcceptInvitationResultDto(tournament.id().value());
      }
      // 招待versionの競合、または同時承諾によるMEMBER重複。状態を読み直して先頭から再試行する
    }
    throw new IllegalStateException("招待の承諾が競合により完了しませんでした: " + token);
  }

  private TournamentInvite loadInvite(String token) {
    if (token == null || !TOKEN_FORMAT.matcher(token).matches()) {
      throw new ForbiddenException(ErrorCode.INVALID_INVITE_TOKEN);
    }
    return inviteRepository
        .findByToken(token)
        .orElseThrow(() -> new ForbiddenException(ErrorCode.INVALID_INVITE_TOKEN));
  }

  private Tournament loadTournament(TournamentInvite invite) {
    return tournamentRepository
        .findById(invite.tournamentId())
        .orElseThrow(() -> new ForbiddenException(ErrorCode.INVALID_INVITE_TOKEN));
  }

  private boolean isMember(Tournament tournament, String sub) {
    return tournament.isOwnedBy(sub)
        || memberRepository.findBySub(tournament.id(), sub).isPresent();
  }
}
