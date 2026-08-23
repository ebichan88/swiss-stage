package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.TournamentInviteDto;
import com.swiss_stage.application.dto.TournamentMemberDto;
import com.swiss_stage.application.dto.TournamentMembersViewDto;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentInvite;
import com.swiss_stage.domain.model.TournamentMemberId;
import com.swiss_stage.domain.repository.TournamentInviteRepository;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 共同管理者の一覧・取り消しと招待リンクの発行・失効(OWNER専用。14_tournament_collaboration.md §4.4・§4.7)。 招待の承諾は{@link
 * InvitationService}が担当する。
 */
@Service
public class TournamentMemberService {

  /** 共同管理者の上限人数(OWNERを含めない。14_tournament_collaboration.md §4.4) */
  static final int MAX_MEMBERS = 9;

  private final TournamentMemberRepository memberRepository;
  private final TournamentInviteRepository inviteRepository;
  private final TournamentAccessSupport access;
  private final Clock clock;

  public TournamentMemberService(
      TournamentMemberRepository memberRepository,
      TournamentInviteRepository inviteRepository,
      TournamentAccessSupport access,
      Clock clock) {
    this.memberRepository = memberRepository;
    this.inviteRepository = inviteRepository;
    this.access = access;
    this.clock = clock;
  }

  public TournamentMembersViewDto list(TournamentId tournamentId, String ownerSub) {
    access.loadOwner(tournamentId, ownerSub);
    return buildView(tournamentId);
  }

  /** 招待リンクの発行・再発行。1大会につき有効な招待は常に1本(再発行は同じアイテムの上書き。 旧トークンは即時無効になり、使用済みの人数枠もリセットされる)。 */
  public TournamentMembersViewDto issueInvite(
      TournamentId tournamentId, String ownerSub, int maxUses) {
    access.loadOwner(tournamentId, ownerSub);
    int currentMemberCount = memberRepository.findByTournamentId(tournamentId).size();
    int allowedMax = MAX_MEMBERS - currentMemberCount;
    if (maxUses > allowedMax) {
      throw new ValidationException("人数枠は現在の共同管理者数を踏まえた上限(" + allowedMax + "人)以下である必要があります");
    }
    Long previousVersion =
        inviteRepository
            .findByTournamentId(tournamentId)
            .map(TournamentInvite::version)
            .orElse(null);
    TournamentInvite invite =
        TournamentInvite.issue(
            tournamentId, ShareTokens.generate(), maxUses, Instant.now(clock), previousVersion);
    inviteRepository.save(invite);
    return buildView(tournamentId);
  }

  /** 招待リンクの失効。未発行でも冪等に成功する */
  public void revokeInvite(TournamentId tournamentId, String ownerSub) {
    access.loadOwner(tournamentId, ownerSub);
    inviteRepository.delete(tournamentId);
  }

  public void remove(TournamentId tournamentId, TournamentMemberId memberId, String ownerSub) {
    access.loadOwner(tournamentId, ownerSub);
    memberRepository
        .findByMemberId(tournamentId, memberId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.TOURNAMENT_MEMBER_NOT_FOUND));
    memberRepository.delete(tournamentId, memberId);
    // 取り消しは招待リンクも道連れにする(§4.4)。取り消し後に同じリンクで復帰できないようにするため
    inviteRepository.delete(tournamentId);
  }

  private TournamentMembersViewDto buildView(TournamentId tournamentId) {
    List<TournamentMemberDto> members =
        memberRepository.findByTournamentId(tournamentId).stream()
            .map(TournamentMemberDto::from)
            .sorted(Comparator.comparing(TournamentMemberDto::joinedAt))
            .toList();
    TournamentInviteDto inviteDto =
        inviteRepository
            .findByTournamentId(tournamentId)
            .filter(invite -> invite.isAcceptable(Instant.now(clock)))
            .map(
                invite ->
                    new TournamentInviteDto(
                        invite.token(),
                        invite.expiresAt().toString(),
                        invite.maxUses(),
                        invite.remainingUses()))
            .orElse(null);
    return new TournamentMembersViewDto(members, inviteDto, MAX_MEMBERS);
  }
}
