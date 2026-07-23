package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.MatchSide;
import com.swiss_stage.domain.model.ResultInputBy;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.RoundRepository;
import com.swiss_stage.domain.repository.TeamMatchRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbTeamMatchRepositoryTest extends DynamoDbRepositoryTestSupport {

    @Autowired TeamMatchRepository repository;
    @Autowired RoundRepository roundRepository;

    @Test
    @DisplayName("団体戦対局を保存すると埋め込みボード結果一覧も含めてラウンド別・全件・ID指定で取得できる"
            + "(RoundアイテムはSK前方一致でも混ざらない)")
    void 保存と取得() {
        TournamentId tournamentId = TournamentId.generate();
        GroupId groupId = GroupId.generate();
        TeamId t1 = TeamId.generate();
        TeamId t2 = TeamId.generate();
        TeamId t3 = TeamId.generate();
        TeamMatch r1m1 = TeamMatch.pairOf(1, 1, t1, t2, 3, groupId);
        TeamMatch r1bye = TeamMatch.byeOf(1, 2, t3, groupId);
        TeamMatch r2m1 = TeamMatch.pairOf(2, 1, t1, t3, 3, groupId);
        roundRepository.create(tournamentId, Round.pairing(1));
        repository.saveAll(tournamentId, List.of(r1m1, r1bye));
        repository.save(tournamentId, r2m1);

        assertThat(repository.findByRound(tournamentId, 1))
                .extracting(TeamMatch::id).containsExactlyInAnyOrder(r1m1.id(), r1bye.id());
        assertThat(repository.findAllByTournamentId(tournamentId)).hasSize(3);

        TeamMatch found = repository.findById(tournamentId, r1m1.id()).orElseThrow();
        assertThat(found.boardResults()).hasSize(3);
        assertThat(found.boardResults()).allSatisfy(b -> assertThat(b.result()).isEqualTo(MatchResult.NONE));

        TeamMatch foundBye = repository.findById(tournamentId, r1bye.id()).orElseThrow();
        assertThat(foundBye.isBye()).isTrue();
        assertThat(foundBye.boardResults()).isEmpty();
        assertThat(foundBye.team2Id()).isNull();

        assertThat(repository.findById(tournamentId, com.swiss_stage.domain.model.TeamMatchId.generate()))
                .isEmpty();
    }

    @Test
    @DisplayName("運営者のボード結果一括入力(部分入力含む)を保存・復元できる")
    void ボード結果の直接確定() {
        TournamentId tournamentId = TournamentId.generate();
        GroupId groupId = GroupId.generate();
        TeamMatch match = TeamMatch.pairOf(1, 1, TeamId.generate(), TeamId.generate(), 3, groupId);
        repository.save(tournamentId, match);

        TeamMatch loaded = repository.findById(tournamentId, match.id()).orElseThrow();
        TeamMatch updated = loaded.withBoardResults(
                List.of(MatchResult.PLAYER1_WIN, MatchResult.NONE, MatchResult.DRAW));
        repository.save(tournamentId, updated);

        TeamMatch found = repository.findById(tournamentId, match.id()).orElseThrow();
        assertThat(found.boardResults().get(0).result()).isEqualTo(MatchResult.PLAYER1_WIN);
        assertThat(found.boardResults().get(1).result()).isEqualTo(MatchResult.NONE);
        assertThat(found.boardResults().get(2).result()).isEqualTo(MatchResult.DRAW);
        assertThat(found.resultInputBy()).isEqualTo(ResultInputBy.OWNER);
    }

    @Test
    @DisplayName("共有トークン経由のボード単位自己申告を保存・復元できる")
    void ボード単位の自己申告() {
        TournamentId tournamentId = TournamentId.generate();
        GroupId groupId = GroupId.generate();
        TeamMatch match = TeamMatch.pairOf(1, 1, TeamId.generate(), TeamId.generate(), 3, groupId);
        repository.save(tournamentId, match);

        TeamMatch loaded = repository.findById(tournamentId, match.id()).orElseThrow();
        TeamMatch reported = loaded.withReportedBoardResults(MatchSide.PLAYER1, List.of(
                MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.DRAW));
        repository.save(tournamentId, reported);

        TeamMatch found = repository.findById(tournamentId, match.id()).orElseThrow();
        assertThat(found.boardResults().get(0).team1ReportedResult()).isEqualTo(MatchResult.PLAYER1_WIN);
        assertThat(found.boardResults().get(0).result()).isEqualTo(MatchResult.NONE); // 片方のみ申告
    }

    @Test
    @DisplayName("結果入力は保存後のversionで更新でき、古いversionは楽観ロック競合になる")
    void 結果入力の楽観ロック() {
        TournamentId tournamentId = TournamentId.generate();
        TeamMatch match =
                TeamMatch.pairOf(1, 1, TeamId.generate(), TeamId.generate(), 3, GroupId.generate());
        repository.save(tournamentId, match);

        TeamMatch loaded = repository.findById(tournamentId, match.id()).orElseThrow();
        assertThat(loaded.version()).isPositive();

        repository.save(tournamentId, loaded.withBoardResults(
                List.of(MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN, MatchResult.PLAYER1_WIN)));

        assertThatThrownBy(() -> repository.save(tournamentId, loaded.withBoardResults(
                        List.of(MatchResult.PLAYER2_WIN, MatchResult.PLAYER2_WIN, MatchResult.PLAYER2_WIN))))
                .isInstanceOf(OptimisticLockException.class);
    }
}
