package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.GeneratedTeamRoundDto;
import com.swiss_stage.application.dto.InputTeamMatchResultRequest;
import com.swiss_stage.application.dto.TeamMatchDto;
import com.swiss_stage.application.dto.TeamRoundDto;
import com.swiss_stage.application.service.TeamRoundService;
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
public class TeamRoundController {

    private final TeamRoundService teamRoundService;
    private final Clock clock;

    public TeamRoundController(TeamRoundService teamRoundService, Clock clock) {
        this.teamRoundService = teamRoundService;
        this.clock = clock;
    }

    @GetMapping("/team-rounds")
    public ApiSuccess<List<TeamRoundDto>> list(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
        return success(teamRoundService.list(PathIds.tournamentId(tournamentId), user.sub()));
    }

    @PostMapping("/team-rounds")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<GeneratedTeamRoundDto> generate(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
        return success(teamRoundService.generateNextRound(
                PathIds.tournamentId(tournamentId), user.sub()));
    }

    @PostMapping("/team-rounds/{roundNumber}/confirm")
    public ApiSuccess<TeamRoundDto> confirm(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("roundNumber") int roundNumber) {
        return success(teamRoundService.confirm(
                PathIds.tournamentId(tournamentId), roundNumber, user.sub()));
    }

    @PutMapping("/team-matches/{matchId}/result")
    public ApiSuccess<TeamMatchDto> inputResult(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("matchId") String matchId,
            @Valid @RequestBody InputTeamMatchResultRequest request) {
        return success(teamRoundService.inputResult(
                PathIds.tournamentId(tournamentId), PathIds.teamMatchId(matchId), user.sub(), request));
    }

    private <T> ApiSuccess<T> success(T data) {
        return ApiSuccess.of(data, Instant.now(clock));
    }
}
