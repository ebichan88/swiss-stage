package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.IssueInviteRequest;
import com.swiss_stage.application.dto.TournamentMembersViewDto;
import com.swiss_stage.application.service.TournamentMemberService;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 招待リンクの発行・再発行・失効(OWNER専用。14_tournament_collaboration.md §4.4・§4.7) */
@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/invite")
public class TournamentInviteController {

  private final TournamentMemberService memberService;
  private final Clock clock;

  public TournamentInviteController(TournamentMemberService memberService, Clock clock) {
    this.memberService = memberService;
    this.clock = clock;
  }

  @PostMapping
  public ApiSuccess<TournamentMembersViewDto> issue(
      CurrentUser user,
      @PathVariable("tournamentId") String tournamentId,
      @Valid @RequestBody IssueInviteRequest request) {
    return success(
        memberService.issueInvite(
            PathIds.tournamentId(tournamentId), user.sub(), request.maxUses()));
  }

  @DeleteMapping
  public ResponseEntity<Void> revoke(
      CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
    memberService.revokeInvite(PathIds.tournamentId(tournamentId), user.sub());
    return ResponseEntity.noContent().build();
  }

  private <T> ApiSuccess<T> success(T data) {
    return ApiSuccess.of(data, Instant.now(clock));
  }
}
