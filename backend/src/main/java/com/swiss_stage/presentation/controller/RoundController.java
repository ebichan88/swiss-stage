package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.GeneratedRoundDto;
import com.swiss_stage.application.dto.InputResultRequest;
import com.swiss_stage.application.dto.MatchDto;
import com.swiss_stage.application.dto.RoundDto;
import com.swiss_stage.application.service.RoundService;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}")
public class RoundController {

    private final RoundService roundService;
    private final Clock clock;

    public RoundController(RoundService roundService, Clock clock) {
        this.roundService = roundService;
        this.clock = clock;
    }

    @GetMapping("/rounds")
    public ApiSuccess<List<RoundDto>> list(CurrentUser user, @PathVariable String tournamentId) {
        return success(roundService.list(PathIds.tournamentId(tournamentId), user.sub()));
    }

    @PostMapping("/rounds")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<GeneratedRoundDto> generate(
            CurrentUser user, @PathVariable String tournamentId) {
        return success(roundService.generateNextRound(
                PathIds.tournamentId(tournamentId), user.sub()));
    }

    @PostMapping("/rounds/{roundNumber}/confirm")
    public ApiSuccess<RoundDto> confirm(
            CurrentUser user, @PathVariable String tournamentId, @PathVariable int roundNumber) {
        return success(roundService.confirm(
                PathIds.tournamentId(tournamentId), roundNumber, user.sub()));
    }

    @PutMapping("/matches/{matchId}/result")
    public ApiSuccess<MatchDto> inputResult(
            CurrentUser user, @PathVariable String tournamentId, @PathVariable String matchId,
            @Valid @RequestBody InputResultRequest request) {
        return success(roundService.inputResult(
                PathIds.tournamentId(tournamentId), PathIds.matchId(matchId), user.sub(), request));
    }

    private <T> ApiSuccess<T> success(T data) {
        return ApiSuccess.of(data, Instant.now(clock));
    }
}
