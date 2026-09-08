package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.AcceptInvitationResultDto;
import com.swiss_stage.application.dto.InvitationPreviewDto;
import com.swiss_stage.application.service.InvitationService;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 招待の受諾(認証済みなら誰でも呼べる。IPベースのレート制限あり= {@code InvitationRateLimitFilter}。
 * 14_tournament_collaboration.md §4.4・§4.7)
 */
@RestController
@RequestMapping("/api/v1/invitations/{token}")
public class InvitationController {

  private final InvitationService invitationService;
  private final Clock clock;

  public InvitationController(InvitationService invitationService, Clock clock) {
    this.invitationService = invitationService;
    this.clock = clock;
  }

  @GetMapping
  public ApiSuccess<InvitationPreviewDto> preview(
      CurrentUser user, @PathVariable("token") String token) {
    return success(invitationService.preview(token, user.sub()));
  }

  @PostMapping("/accept")
  public ApiSuccess<AcceptInvitationResultDto> accept(
      CurrentUser user, @PathVariable("token") String token) {
    return success(invitationService.accept(token, user.sub(), user.name()));
  }

  private <T> ApiSuccess<T> success(T data) {
    return ApiSuccess.of(data, Instant.now(clock));
  }
}
