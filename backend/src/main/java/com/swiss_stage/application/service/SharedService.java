package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.InputResultRequest;
import com.swiss_stage.application.dto.MatchDto;
import com.swiss_stage.application.dto.SharedTournamentDto;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.ForbiddenException;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.ResultInputBy;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.Visibility;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 共有トークン経由のアクセス(13_security_design.md §2-3)。
 * 無効・不明・非公開はすべて同じ INVALID_SHARE_TOKEN(403)で返し、大会の存在を漏らさない。
 */
@Service
public class SharedService {

    /** URL-safe Base64(ShareTokens.generate は43文字)。形式不正はキー組み立てに使わない */
    private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{32,64}");

    private final TournamentRepository tournamentRepository;
    private final RoundService roundService;
    private final StandingService standingService;

    public SharedService(
            TournamentRepository tournamentRepository,
            RoundService roundService,
            StandingService standingService) {
        this.tournamentRepository = tournamentRepository;
        this.roundService = roundService;
        this.standingService = standingService;
    }

    /** 共有ページ(S10)用の集約。shareToken・ownerSub は含めない */
    public SharedTournamentDto getShared(String token) {
        Tournament tournament = resolveByToken(token);
        return new SharedTournamentDto(
                SharedTournamentDto.SharedTournamentSummary.from(tournament),
                roundService.assembleRounds(tournament.id()),
                standingService.assembleStandings(tournament.id()));
    }

    /** 共有トークン経由の結果入力。大会設定(resultInputEnabled)で許可時のみ */
    public MatchDto inputResult(String token, MatchId matchId, InputResultRequest request) {
        Tournament tournament = resolveByToken(token);
        if (!tournament.resultInputEnabled()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
        return roundService.applyResult(
                tournament.id(), matchId, request, ResultInputBy.SHARE_TOKEN);
    }

    private Tournament resolveByToken(String token) {
        if (token == null || !TOKEN_FORMAT.matcher(token).matches()) {
            throw new ForbiddenException(ErrorCode.INVALID_SHARE_TOKEN);
        }
        Tournament tournament = tournamentRepository.findByShareToken(token)
                .orElseThrow(() -> new ForbiddenException(ErrorCode.INVALID_SHARE_TOKEN));
        if (tournament.visibility() == Visibility.PRIVATE) {
            throw new ForbiddenException(ErrorCode.INVALID_SHARE_TOKEN);
        }
        return tournament;
    }
}
