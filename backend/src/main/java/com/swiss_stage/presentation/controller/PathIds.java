package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatchId;
import com.swiss_stage.domain.model.TeamMemberId;
import com.swiss_stage.domain.model.TournamentId;
import java.util.regex.Pattern;

/**
 * パスパラメータのID変換。ULID形式(26文字のCrockford Base32)を検証し、
 * 不正な形式は404にする(キー組み立てへのユーザー入力混入禁止: 13_security_design.md §5)。
 */
final class PathIds {

    private static final Pattern ULID = Pattern.compile("[0-9A-HJKMNP-TV-Z]{26}");

    private PathIds() {}

    static TournamentId tournamentId(String value) {
        requireUlid(value, ErrorCode.TOURNAMENT_NOT_FOUND);
        return new TournamentId(value);
    }

    static ParticipantId participantId(String value) {
        requireUlid(value, ErrorCode.PARTICIPANT_NOT_FOUND);
        return new ParticipantId(value);
    }

    static MatchId matchId(String value) {
        requireUlid(value, ErrorCode.MATCH_NOT_FOUND);
        return new MatchId(value);
    }

    static GroupId groupId(String value) {
        requireUlid(value, ErrorCode.GROUP_NOT_FOUND);
        return new GroupId(value);
    }

    static TeamId teamId(String value) {
        requireUlid(value, ErrorCode.TEAM_NOT_FOUND);
        return new TeamId(value);
    }

    static TeamMemberId teamMemberId(String value) {
        requireUlid(value, ErrorCode.TEAM_MEMBER_NOT_FOUND);
        return new TeamMemberId(value);
    }

    static TeamMatchId teamMatchId(String value) {
        requireUlid(value, ErrorCode.TEAM_MATCH_NOT_FOUND);
        return new TeamMatchId(value);
    }

    private static void requireUlid(String value, ErrorCode notFound) {
        if (value == null || !ULID.matcher(value).matches()) {
            throw new NotFoundException(notFound);
        }
    }
}
