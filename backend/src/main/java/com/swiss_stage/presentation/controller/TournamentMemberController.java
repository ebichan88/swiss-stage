package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.TournamentMembersViewDto;
import com.swiss_stage.application.service.TournamentMemberService;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 共同管理者の一覧・取り消し(OWNER専用。14_tournament_collaboration.md §4.7) */
@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/members")
public class TournamentMemberController {

  private final TournamentMemberService memberService;
  private final Clock clock;

  public TournamentMemberController(TournamentMemberService memberService, Clock clock) {
    this.memberService = memberService;
    this.clock = clock;
  }

  @GetMapping
  public ApiSuccess<TournamentMembersViewDto> list(
      CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
    return success(memberService.list(PathIds.tournamentId(tournamentId), user.sub()));
  }

  @DeleteMapping("/{memberId}")
  public ResponseEntity<Void> remove(
      CurrentUser user,
      @PathVariable("tournamentId") String tournamentId,
      @PathVariable("memberId") String memberId) {
    memberService.remove(
        PathIds.tournamentId(tournamentId), PathIds.tournamentMemberId(memberId), user.sub());
    return ResponseEntity.noContent().build();
  }

  private <T> ApiSuccess<T> success(T data) {
    return ApiSuccess.of(data, Instant.now(clock));
  }
}
