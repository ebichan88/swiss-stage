package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.ResultInputBy;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.MatchRepository;
import com.swiss_stage.domain.repository.RoundRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbMatchRepositoryTest extends DynamoDbRepositoryTestSupport {

    @Autowired MatchRepository repository;
    @Autowired RoundRepository roundRepository;

    @Test
    @DisplayName("対局を保存してラウンド別・全件・ID指定で取得できる(RoundアイテムはSK前方一致でも混ざらない)")
    void 保存と取得() {
        TournamentId tournamentId = TournamentId.generate();
        GroupId groupId = GroupId.generate();
        ParticipantId p1 = ParticipantId.generate();
        ParticipantId p2 = ParticipantId.generate();
        ParticipantId p3 = ParticipantId.generate();
        Match r1m1 = Match.pairOf(1, 1, p1, p2, groupId);
        Match r1bye = Match.byeOf(1, 2, p3, groupId);
        Match r2m1 = Match.pairOf(2, 1, p1, p3, groupId);
        // ROUND#プレフィックスを共有するRoundアイテムが混入しないことを確認するために作る
        roundRepository.create(tournamentId, Round.pairing(1));
        repository.saveAll(tournamentId, List.of(r1m1, r1bye));
        repository.save(tournamentId, r2m1);

        assertThat(repository.findByRound(tournamentId, 1))
                .extracting(Match::id).containsExactlyInAnyOrder(r1m1.id(), r1bye.id());
        assertThat(repository.findAllByTournamentId(tournamentId)).hasSize(3);

        Match found = repository.findById(tournamentId, r1bye.id()).orElseThrow();
        assertThat(found.isBye()).isTrue();
        assertThat(found.result()).isEqualTo(MatchResult.BYE);
        assertThat(found.player2Id()).isNull();
        assertThat(repository.findById(tournamentId, com.swiss_stage.domain.model.MatchId.generate()))
                .isEmpty();
    }

    @Test
    @DisplayName("対局のグループ帰属を保存して復元できる")
    void グループ帰属の往復() {
        TournamentId tournamentId = TournamentId.generate();
        GroupId groupId = GroupId.generate();
        Match grouped = Match.pairOf(1, 1, ParticipantId.generate(), ParticipantId.generate(), groupId);
        Match groupedBye = Match.byeOf(1, 2, ParticipantId.generate(), groupId);
        repository.saveAll(tournamentId, List.of(grouped, groupedBye));

        assertThat(repository.findById(tournamentId, grouped.id()).orElseThrow().groupId())
                .isEqualTo(groupId);
        assertThat(repository.findById(tournamentId, groupedBye.id()).orElseThrow().groupId())
                .isEqualTo(groupId);

        // 結果入力してもグループ帰属は保たれる
        Match loaded = repository.findById(tournamentId, grouped.id()).orElseThrow();
        repository.save(tournamentId, loaded.withResult(MatchResult.PLAYER1_WIN));
        assertThat(repository.findById(tournamentId, grouped.id()).orElseThrow().groupId())
                .isEqualTo(groupId);
    }

    @Test
    @DisplayName("結果入力は保存後のversionで更新でき、古いversionは楽観ロック競合になる")
    void 結果入力の楽観ロック() {
        TournamentId tournamentId = TournamentId.generate();
        Match match = Match.pairOf(
                1, 1, ParticipantId.generate(), ParticipantId.generate(), GroupId.generate());
        repository.save(tournamentId, match);

        Match loaded = repository.findById(tournamentId, match.id()).orElseThrow();
        assertThat(loaded.version()).isPositive();
        assertThat(loaded.resultInputBy()).isNull();
        repository.save(tournamentId,
                loaded.withResult(MatchResult.PLAYER1_WIN, ResultInputBy.SHARE_TOKEN));

        Match updated = repository.findById(tournamentId, match.id()).orElseThrow();
        assertThat(updated.result()).isEqualTo(MatchResult.PLAYER1_WIN);
        assertThat(updated.resultInputBy()).isEqualTo(ResultInputBy.SHARE_TOKEN);
        assertThat(updated.version()).isGreaterThan(loaded.version());

        // 古いversionのまま上書きしようとすると競合
        assertThatThrownBy(() -> repository.save(tournamentId, loaded.withResult(MatchResult.DRAW)))
                .isInstanceOf(OptimisticLockException.class);
    }
}
