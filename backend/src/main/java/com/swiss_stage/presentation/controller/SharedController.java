package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.MatchDto;
import com.swiss_stage.application.dto.ReportMatchResultRequest;
import com.swiss_stage.application.dto.SharedTournamentDto;
import com.swiss_stage.application.service.SharedService;
import com.swiss_stage.presentation.api.ApiSuccess;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共有トークン経由のアクセス(S10/S11。認証不要・SharedRateLimitFilterでIPレート制限)。
 * トークンの検証・公開範囲・結果入力許可のチェックは SharedService が行う。
 */
@RestController
@RequestMapping("/api/v1/shared")
public class SharedController {

    private final SharedService sharedService;
    private final Clock clock;

    public SharedController(SharedService sharedService, Clock clock) {
        this.sharedService = sharedService;
        this.clock = clock;
    }

    @GetMapping("/{token}")
    public ApiSuccess<SharedTournamentDto> get(@PathVariable("token") String token) {
        return ApiSuccess.of(sharedService.getShared(token), Instant.now(clock));
    }

    @PutMapping("/{token}/matches/{matchId}/result")
    public ApiSuccess<MatchDto> inputResult(
            @PathVariable("token") String token,
            @PathVariable("matchId") String matchId,
            @Valid @RequestBody ReportMatchResultRequest request) {
        return ApiSuccess.of(
                sharedService.inputResult(token, PathIds.matchId(matchId), request),
                Instant.now(clock));
    }
}
