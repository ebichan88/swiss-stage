package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.AddTeamMemberRequest;
import com.swiss_stage.application.dto.CreateTeamRequest;
import com.swiss_stage.application.dto.TeamCsvImportResultDto;
import com.swiss_stage.application.dto.TeamDto;
import com.swiss_stage.application.dto.UpdateTeamMemberRequest;
import com.swiss_stage.application.dto.UpdateTeamRequest;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.application.service.TeamService;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/teams")
public class TeamController {

    private final TeamService teamService;
    private final Clock clock;

    public TeamController(TeamService teamService, Clock clock) {
        this.teamService = teamService;
        this.clock = clock;
    }

    @GetMapping
    public ApiSuccess<List<TeamDto>> list(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
        return success(teamService.list(PathIds.tournamentId(tournamentId), user.sub()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<TeamDto> create(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @Valid @RequestBody CreateTeamRequest request) {
        return success(teamService.create(PathIds.tournamentId(tournamentId), user.sub(), request));
    }

    @PostMapping("/csv-import")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<TeamCsvImportResultDto> importCsv(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("CSVファイルを選択してください");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return success(teamService.importCsv(PathIds.tournamentId(tournamentId), user.sub(), bytes));
    }

    @PatchMapping("/{teamId}")
    public ApiSuccess<TeamDto> update(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("teamId") String teamId,
            @Valid @RequestBody UpdateTeamRequest request) {
        return success(teamService.update(
                PathIds.tournamentId(tournamentId), PathIds.teamId(teamId), user.sub(), request));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> delete(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("teamId") String teamId) {
        teamService.delete(PathIds.tournamentId(tournamentId), PathIds.teamId(teamId), user.sub());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<TeamDto> addMember(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("teamId") String teamId,
            @Valid @RequestBody AddTeamMemberRequest request) {
        return success(teamService.addMember(
                PathIds.tournamentId(tournamentId), PathIds.teamId(teamId), user.sub(), request));
    }

    @PatchMapping("/{teamId}/members/{memberId}")
    public ApiSuccess<TeamDto> updateMember(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("teamId") String teamId, @PathVariable("memberId") String memberId,
            @Valid @RequestBody UpdateTeamMemberRequest request) {
        return success(teamService.updateMember(
                PathIds.tournamentId(tournamentId), PathIds.teamId(teamId),
                PathIds.teamMemberId(memberId), user.sub(), request));
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ApiSuccess<TeamDto> deleteMember(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("teamId") String teamId, @PathVariable("memberId") String memberId) {
        return success(teamService.deleteMember(
                PathIds.tournamentId(tournamentId), PathIds.teamId(teamId),
                PathIds.teamMemberId(memberId), user.sub()));
    }

    private <T> ApiSuccess<T> success(T data) {
        return ApiSuccess.of(data, Instant.now(clock));
    }
}
