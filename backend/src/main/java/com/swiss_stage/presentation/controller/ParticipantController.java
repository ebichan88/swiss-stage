package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.CreateParticipantRequest;
import com.swiss_stage.application.dto.CsvImportResultDto;
import com.swiss_stage.application.dto.ParticipantDto;
import com.swiss_stage.application.dto.UpdateParticipantRequest;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.application.service.ParticipantService;
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
@RequestMapping("/api/v1/tournaments/{tournamentId}/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    private final Clock clock;

    public ParticipantController(ParticipantService participantService, Clock clock) {
        this.participantService = participantService;
        this.clock = clock;
    }

    @GetMapping
    public ApiSuccess<List<ParticipantDto>> list(
            CurrentUser user, @PathVariable String tournamentId) {
        return success(participantService.list(PathIds.tournamentId(tournamentId), user.sub()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<ParticipantDto> add(
            CurrentUser user, @PathVariable String tournamentId,
            @Valid @RequestBody CreateParticipantRequest request) {
        return success(participantService.add(
                PathIds.tournamentId(tournamentId), user.sub(), request));
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccess<CsvImportResultDto> importCsv(
            CurrentUser user, @PathVariable String tournamentId,
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
        return success(participantService.importCsv(
                PathIds.tournamentId(tournamentId), user.sub(), bytes));
    }

    @PatchMapping("/{participantId}")
    public ApiSuccess<ParticipantDto> update(
            CurrentUser user, @PathVariable String tournamentId,
            @PathVariable String participantId,
            @Valid @RequestBody UpdateParticipantRequest request) {
        return success(participantService.update(
                PathIds.tournamentId(tournamentId), PathIds.participantId(participantId),
                user.sub(), request));
    }

    @DeleteMapping("/{participantId}")
    public ResponseEntity<Void> delete(
            CurrentUser user, @PathVariable String tournamentId,
            @PathVariable String participantId) {
        participantService.delete(
                PathIds.tournamentId(tournamentId), PathIds.participantId(participantId), user.sub());
        return ResponseEntity.noContent().build();
    }

    private <T> ApiSuccess<T> success(T data) {
        return ApiSuccess.of(data, Instant.now(clock));
    }
}
