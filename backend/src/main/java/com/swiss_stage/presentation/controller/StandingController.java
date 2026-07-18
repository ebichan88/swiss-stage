package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.GroupStandingsDto;
import com.swiss_stage.application.service.StandingService;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/standings")
public class StandingController {

    private final StandingService standingService;
    private final Clock clock;

    public StandingController(StandingService standingService, Clock clock) {
        this.standingService = standingService;
        this.clock = clock;
    }

    @GetMapping
    public ApiSuccess<List<GroupStandingsDto>> standings(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
        return ApiSuccess.of(
                standingService.standings(PathIds.tournamentId(tournamentId), user.sub()),
                Instant.now(clock));
    }
}
