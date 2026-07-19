package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.model.Visibility;
import com.swiss_stage.domain.repository.MatchRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.repository.RoundRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbTournamentRepositoryTest extends DynamoDbRepositoryTestSupport {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    @Autowired TournamentRepository repository;
    @Autowired ParticipantRepository participantRepository;
    @Autowired RoundRepository roundRepository;
    @Autowired MatchRepository matchRepository;

    @Test
    @DisplayName("大会を保存して全属性を復元できる(初回保存でversionが払い出される)")
    void 保存と取得() {
        Tournament tournament = Tournament.create("統合テスト大会", GameType.GO, 5, uniqueSub(), NOW)
                .withShareToken(uniqueToken())
                .withResultInputEnabled(true);
        repository.save(tournament);

        Tournament found = repository.findById(tournament.id()).orElseThrow();
        assertThat(found.id()).isEqualTo(tournament.id());
        assertThat(found.name()).isEqualTo("統合テスト大会");
        assertThat(found.gameType()).isEqualTo(GameType.GO);
        assertThat(found.totalRounds()).isEqualTo(5);
        assertThat(found.currentRound()).isZero();
        assertThat(found.status()).isEqualTo(TournamentStatus.PREPARING);
        assertThat(found.visibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(found.shareToken()).isEqualTo(tournament.shareToken());
        assertThat(found.resultInputEnabled()).isTrue();
        assertThat(found.ownerSub()).isEqualTo(tournament.ownerSub());
        assertThat(found.createdAt()).isEqualTo(NOW);
        assertThat(found.updatedAt()).isEqualTo(NOW);
        assertThat(found.version()).isPositive();
    }

    @Test
    @DisplayName("存在しない大会はemptyを返す")
    void 未存在() {
        assertThat(repository.findById(
                com.swiss_stage.domain.model.TournamentId.generate())).isEmpty();
    }

    @Test
    @DisplayName("古いversionでの保存は楽観ロック競合になる")
    void 楽観ロック() {
        Tournament tournament = Tournament.create("競合テスト", GameType.SHOGI, 3, uniqueSub(), NOW);
        repository.save(tournament);

        Tournament loaded = repository.findById(tournament.id()).orElseThrow();
        repository.save(loaded.rename("更新1").touched(NOW.plusSeconds(1)));

        // loadedのversionは古くなっているので競合する
        assertThatThrownBy(() -> repository.save(loaded.rename("更新2")))
                .isInstanceOf(OptimisticLockException.class);
        // 未保存扱い(version 0)の同一IDも新規条件に反して競合する
        assertThatThrownBy(() -> repository.save(tournament))
                .isInstanceOf(OptimisticLockException.class);
    }

    @Test
    @DisplayName("運営者の大会一覧を新しい順で取得できる(GSI1)")
    void 運営者の大会一覧() {
        String sub = uniqueSub();
        Tournament older = Tournament.create("古い大会", GameType.GO, 3, sub, NOW);
        Tournament newer = Tournament.create("新しい大会", GameType.GO, 3, sub, NOW.plusSeconds(60));
        repository.save(older);
        repository.save(newer);

        List<Tournament> found = awaitNonEmpty(() -> {
            List<Tournament> list = repository.findByOwnerSub(sub);
            return list.size() == 2 ? list : List.of();
        });
        assertThat(found).extracting(Tournament::name).containsExactly("新しい大会", "古い大会");
    }

    @Test
    @DisplayName("共有トークンから大会を特定できる(GSI2)")
    void 共有トークン検索() {
        String token = uniqueToken();
        Tournament tournament = Tournament.create("共有大会", GameType.GO, 3, uniqueSub(), NOW)
                .withShareToken(token);
        repository.save(tournament);

        List<Tournament> found = awaitNonEmpty(() ->
                repository.findByShareToken(token).map(List::of).orElse(List.of()));
        assertThat(found.getFirst().id()).isEqualTo(tournament.id());
        assertThat(repository.findByShareToken("no-such-token-" + UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("大会削除で配下の参加者・ラウンド・対局も物理削除される")
    void 一括削除() {
        Tournament tournament = Tournament.create("削除対象", GameType.GO, 3, uniqueSub(), NOW);
        repository.save(tournament);
        GroupId groupId = GroupId.generate();
        Participant p1 = Participant.create("削除 一郎", "A社", Rank.DAN_1, 1, groupId);
        Participant p2 = Participant.create("削除 二郎", null, null, 2, groupId);
        participantRepository.saveAll(tournament.id(), List.of(p1, p2));
        roundRepository.create(tournament.id(), Round.pairing(1));
        matchRepository.save(tournament.id(), Match.pairOf(1, 1, p1.id(), p2.id(), groupId));

        repository.delete(tournament.id());

        assertThat(repository.findById(tournament.id())).isEmpty();
        assertThat(participantRepository.findAllByTournamentId(tournament.id())).isEmpty();
        assertThat(roundRepository.findAllByTournamentId(tournament.id())).isEmpty();
        assertThat(matchRepository.findAllByTournamentId(tournament.id())).isEmpty();
    }

    private static String uniqueSub() {
        return "sub-" + UUID.randomUUID();
    }

    private static String uniqueToken() {
        return "tok-" + UUID.randomUUID();
    }
}
