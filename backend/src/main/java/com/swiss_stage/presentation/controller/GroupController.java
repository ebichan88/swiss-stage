package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.GroupDto;
import com.swiss_stage.application.dto.GroupRequest;
import com.swiss_stage.application.dto.ParticipantDto;
import com.swiss_stage.application.service.GroupService;
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
@RequestMapping("/api/v1/tournaments/{tournamentId}/groups")
public class GroupController {

    private final GroupService groupService;
    private final Clock clock;

    public GroupController(GroupService groupService, Clock clock) {
        this.groupService = groupService;
        this.clock = clock;
    }

    @GetMapping
    public ApiSuccess<List<GroupDto>> list(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
        return success(groupService.list(PathIds.tournamentId(tournamentId), user.sub()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<GroupDto> create(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @Valid @RequestBody GroupRequest request) {
        return success(groupService.create(
                PathIds.tournamentId(tournamentId), user.sub(), request));
    }

    @PatchMapping("/{groupId}")
    public ApiSuccess<GroupDto> rename(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("groupId") String groupId,
            @Valid @RequestBody GroupRequest request) {
        return success(groupService.rename(
                PathIds.tournamentId(tournamentId), PathIds.groupId(groupId), user.sub(), request));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId,
            @PathVariable("groupId") String groupId) {
        groupService.delete(
                PathIds.tournamentId(tournamentId), PathIds.groupId(groupId), user.sub());
        return ResponseEntity.noContent().build();
    }

    /** 段級位による一括振り分け(05_swiss_pairing_algorithm.md §2.4)。更新後の参加者一覧を返す */
    @PostMapping("/auto-assign")
    public ApiSuccess<List<ParticipantDto>> autoAssign(
            CurrentUser user, @PathVariable("tournamentId") String tournamentId) {
        return success(groupService.autoAssign(PathIds.tournamentId(tournamentId), user.sub()));
    }

    private <T> ApiSuccess<T> success(T data) {
        return ApiSuccess.of(data, Instant.now(clock));
    }
}
