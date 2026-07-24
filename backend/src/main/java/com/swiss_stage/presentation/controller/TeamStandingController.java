package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.GroupTeamStandingsDto;
import com.swiss_stage.application.service.TeamStandingService;
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
@RequestMapping("/api/v1/tournaments/{tournamentId}/team-standings")
public class TeamStandingController {

    private final TeamStandingService teamStandingService;
    private final Clock clock;

    public TeamStandingController(TeamStandingService teamStandingService, Clock clock) {
        this.teamStandingService = teamStandingService;
        this.clock = clock;
    }

    @GetMapping
    public ApiSuccess<List<GroupTeamStandingsDto>> standings(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
        return ApiSuccess.of(
                teamStandingService.standings(PathIds.tournamentId(tournamentId), user.sub()),
                Instant.now(clock));
    }
}
