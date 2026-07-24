package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.MatchDto;
import com.swiss_stage.application.dto.ReportMatchResultRequest;
import com.swiss_stage.application.dto.ReportTeamMatchResultRequest;
import com.swiss_stage.application.dto.SharedTournamentDto;
import com.swiss_stage.application.dto.TeamMatchDto;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.ForbiddenException;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.TeamMatchId;
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
    private final TeamRoundService teamRoundService;
    private final TeamStandingService teamStandingService;
    private final SharedViewCache cache;

    public SharedService(
            TournamentRepository tournamentRepository,
            RoundService roundService,
            StandingService standingService,
            TeamRoundService teamRoundService,
            TeamStandingService teamStandingService,
            SharedViewCache cache) {
        this.tournamentRepository = tournamentRepository;
        this.roundService = roundService;
        this.standingService = standingService;
        this.teamRoundService = teamRoundService;
        this.teamStandingService = teamStandingService;
        this.cache = cache;
    }

    /**
     * 共有ページ(S10)用の集約。shareToken・ownerSub は含めない。
     * ラウンド確定直後のアクセススパイクに備えキャッシュする(無効トークンはキャッシュされない)。
     * competitionTypeに応じてrounds/standingsかteamRounds/teamStandingsのどちらかを埋める。
     */
    public SharedTournamentDto getShared(String token) {
        return cache.get(token, t -> {
            Tournament tournament = resolveByToken(t);
            SharedTournamentDto dto = tournament.isTeamCompetition()
                    ? new SharedTournamentDto(
                            SharedTournamentDto.SharedTournamentSummary.from(tournament),
                            null, null,
                            teamRoundService.assembleRounds(tournament.id()),
                            teamStandingService.assembleStandings(tournament.id()))
                    : new SharedTournamentDto(
                            SharedTournamentDto.SharedTournamentSummary.from(tournament),
                            roundService.assembleRounds(tournament.id()),
                            standingService.assembleStandings(tournament.id()),
                            null, null);
            return new SharedViewCache.Entry(tournament.id(), dto);
        });
    }

    /**
     * 共有トークン経由の結果自己申告(個人戦)。大会設定(resultInputEnabled)で許可時のみ。
     * 両者の申告が一致すると自動確定する(RoundService#applyReport)
     */
    public MatchDto inputResult(String token, MatchId matchId, ReportMatchResultRequest request) {
        Tournament tournament = resolveByToken(token);
        if (!tournament.resultInputEnabled()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
        return roundService.applyReport(tournament.id(), matchId, request);
    }

    /**
     * 共有トークン経由のボード単位自己申告(団体戦)。大会設定(resultInputEnabled)で許可時のみ。
     * ボードごとに両者の申告が一致すると、そのボードの結果が自動確定する(TeamRoundService#applyReport)
     */
    public TeamMatchDto inputTeamMatchResult(
            String token, TeamMatchId matchId, ReportTeamMatchResultRequest request) {
        Tournament tournament = resolveByToken(token);
        if (!tournament.resultInputEnabled()) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }
        return teamRoundService.applyReport(tournament.id(), matchId, request);
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
