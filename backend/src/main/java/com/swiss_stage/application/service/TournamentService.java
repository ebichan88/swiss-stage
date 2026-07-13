package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.CreateTournamentRequest;
import com.swiss_stage.application.dto.TournamentDto;
import com.swiss_stage.application.dto.UpdateTournamentRequest;
import com.swiss_stage.application.exception.ConflictException;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final ParticipantRepository participantRepository;
    private final TournamentAccessSupport access;
    private final Clock clock;

    public TournamentService(
            TournamentRepository tournamentRepository,
            ParticipantRepository participantRepository,
            TournamentAccessSupport access,
            Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.participantRepository = participantRepository;
        this.access = access;
        this.clock = clock;
    }

    public List<TournamentDto> list(String ownerSub) {
        return tournamentRepository.findByOwnerSub(ownerSub).stream()
                .map(TournamentDto::from)
                .toList();
    }

    public TournamentDto create(String ownerSub, CreateTournamentRequest request) {
        Tournament tournament = Tournament.create(
                request.name(), request.gameType(), request.totalRounds(), ownerSub,
                Instant.now(clock));
        tournamentRepository.save(tournament);
        return reload(tournament.id());
    }

    public TournamentDto get(TournamentId id, String ownerSub) {
        return TournamentDto.from(access.loadOwned(id, ownerSub));
    }

    public TournamentDto update(TournamentId id, String ownerSub, UpdateTournamentRequest request) {
        Tournament tournament = access.loadOwned(id, ownerSub);
        if (tournament.version() != request.version()) {
            throw new ConflictException();
        }
        if (request.name() != null) {
            tournament = tournament.rename(request.name());
        }
        if (request.visibility() != null) {
            tournament = tournament.withVisibility(request.visibility());
        }
        tournamentRepository.save(tournament.touched(Instant.now(clock)));
        return reload(id);
    }

    public void delete(TournamentId id, String ownerSub) {
        access.loadOwned(id, ownerSub);
        tournamentRepository.delete(id);
    }

    public TournamentDto start(TournamentId id, String ownerSub) {
        Tournament tournament = access.loadOwned(id, ownerSub);
        long activeCount = participantRepository.findAllByTournamentId(id).stream()
                .filter(Participant::isActive)
                .count();
        if (activeCount < 2) {
            throw new InvalidStateException("大会の開始には参加者が2名以上必要です");
        }
        tournamentRepository.save(tournament.start().touched(Instant.now(clock)));
        return reload(id);
    }

    public TournamentDto finish(TournamentId id, String ownerSub) {
        Tournament tournament = access.loadOwned(id, ownerSub);
        tournamentRepository.save(tournament.finish().touched(Instant.now(clock)));
        return reload(id);
    }

    /** 保存でversionが進むため、レスポンスは保存後の状態を読み直して返す */
    private TournamentDto reload(TournamentId id) {
        return tournamentRepository.findById(id)
                .map(TournamentDto::from)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TOURNAMENT_NOT_FOUND));
    }
}
