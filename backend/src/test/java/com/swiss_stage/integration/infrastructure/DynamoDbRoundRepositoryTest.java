package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.DuplicateRoundException;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.RoundStatus;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.MatchRepository;
import com.swiss_stage.domain.repository.RoundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbRoundRepositoryTest extends DynamoDbRepositoryTestSupport {

    @Autowired RoundRepository repository;
    @Autowired MatchRepository matchRepository;

    @Test
    @DisplayName("ラウンドを作成・取得でき、同一ラウンドの二重生成は拒否される")
    void 作成と二重生成防止() {
        TournamentId tournamentId = TournamentId.generate();
        repository.create(tournamentId, Round.pairing(1));

        Round found = repository.findByRoundNumber(tournamentId, 1).orElseThrow();
        assertThat(found.status()).isEqualTo(RoundStatus.PAIRING);

        assertThatThrownBy(() -> repository.create(tournamentId, Round.pairing(1)))
                .isInstanceOf(DuplicateRoundException.class);
        assertThat(repository.findByRoundNumber(tournamentId, 2)).isEmpty();
    }

    @Test
    @DisplayName("状態更新と一覧取得ができる(MatchアイテムはSK前方一致でも混ざらない)")
    void 更新と一覧() {
        TournamentId tournamentId = TournamentId.generate();
        Round round1 = Round.pairing(1);
        repository.create(tournamentId, round1);
        repository.save(tournamentId, round1.startPlaying());
        repository.create(tournamentId, Round.pairing(2));
        matchRepository.save(tournamentId,
                Match.pairOf(1, 1, ParticipantId.generate(), ParticipantId.generate(),
                        GroupId.generate()));

        assertThat(repository.findByRoundNumber(tournamentId, 1).orElseThrow().status())
                .isEqualTo(RoundStatus.PLAYING);
        assertThat(repository.findAllByTournamentId(tournamentId))
                .extracting(Round::roundNumber).containsExactly(1, 2);
    }
}
