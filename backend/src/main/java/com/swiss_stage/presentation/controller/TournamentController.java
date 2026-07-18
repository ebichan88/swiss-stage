package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.CreateTournamentRequest;
import com.swiss_stage.application.dto.TournamentDto;
import com.swiss_stage.application.dto.UpdateTournamentRequest;
import com.swiss_stage.application.service.TournamentService;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final Clock clock;

    public TournamentController(TournamentService tournamentService, Clock clock) {
        this.tournamentService = tournamentService;
        this.clock = clock;
    }

    @GetMapping
    public ApiSuccess<List<TournamentDto>> list(CurrentUser user) {
        return success(tournamentService.list(user.sub()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<TournamentDto> create(
            CurrentUser user, @Valid @RequestBody CreateTournamentRequest request) {
        return success(tournamentService.create(user.sub(), request));
    }

    @GetMapping("/{id}")
    public ApiSuccess<TournamentDto> get(CurrentUser user, @PathVariable("id") String id) {
        return success(tournamentService.get(PathIds.tournamentId(id), user.sub()));
    }

    @PatchMapping("/{id}")
    public ApiSuccess<TournamentDto> update(
            CurrentUser user, @PathVariable("id") String id,
            @Valid @RequestBody UpdateTournamentRequest request) {
        return success(tournamentService.update(PathIds.tournamentId(id), user.sub(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable("id") String id) {
        tournamentService.delete(PathIds.tournamentId(id), user.sub());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ApiSuccess<TournamentDto> start(CurrentUser user, @PathVariable("id") String id) {
        return success(tournamentService.start(PathIds.tournamentId(id), user.sub()));
    }

    @PostMapping("/{id}/finish")
    public ApiSuccess<TournamentDto> finish(CurrentUser user, @PathVariable("id") String id) {
        return success(tournamentService.finish(PathIds.tournamentId(id), user.sub()));
    }

    /** 共有トークンの発行・再発行(旧トークンは即時無効) */
    @PostMapping("/{id}/share-token/regenerate")
    public ApiSuccess<TournamentDto> regenerateShareToken(
            CurrentUser user, @PathVariable("id") String id) {
        return success(tournamentService.regenerateShareToken(PathIds.tournamentId(id), user.sub()));
    }

    private <T> ApiSuccess<T> success(T data) {
        return ApiSuccess.of(data, Instant.now(clock));
    }
}
