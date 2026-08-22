package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.TournamentMemberDto;
import com.swiss_stage.application.dto.TournamentMembersViewDto;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentMemberId;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import java.util.Comparator;
import org.springframework.stereotype.Service;

/**
 * 共同管理者の一覧・取り消し(OWNER専用。14_tournament_collaboration.md §4.7)。
 * 招待リンクの発行・失効・承諾は別PRで実装するため、一覧のinviteは常にnullを返す。
 */
@Service
public class TournamentMemberService {

  /** 共同管理者の上限人数(OWNERを含めない。14_tournament_collaboration.md §4.4) */
  static final int MAX_MEMBERS = 9;

  private final TournamentMemberRepository memberRepository;
  private final TournamentAccessSupport access;

  public TournamentMemberService(
      TournamentMemberRepository memberRepository, TournamentAccessSupport access) {
    this.memberRepository = memberRepository;
    this.access = access;
  }

  public TournamentMembersViewDto list(TournamentId tournamentId, String ownerSub) {
    access.loadOwner(tournamentId, ownerSub);
    return buildView(tournamentId);
  }

  public void remove(TournamentId tournamentId, TournamentMemberId memberId, String ownerSub) {
    access.loadOwner(tournamentId, ownerSub);
    memberRepository
        .findByMemberId(tournamentId, memberId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.TOURNAMENT_MEMBER_NOT_FOUND));
    memberRepository.delete(tournamentId, memberId);
  }

  private TournamentMembersViewDto buildView(TournamentId tournamentId) {
    var members =
        memberRepository.findByTournamentId(tournamentId).stream()
            .map(TournamentMemberDto::from)
            .sorted(Comparator.comparing(TournamentMemberDto::joinedAt))
            .toList();
    // 招待リンクは別PRで実装するため、現時点では常にnull
    return new TournamentMembersViewDto(members, null, MAX_MEMBERS);
  }
}
