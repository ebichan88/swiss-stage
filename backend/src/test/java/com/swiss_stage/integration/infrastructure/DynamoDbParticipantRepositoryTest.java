package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.ParticipantRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbParticipantRepositoryTest extends DynamoDbRepositoryTestSupport {

    @Autowired ParticipantRepository repository;

    private final GroupId groupId = GroupId.generate();

    @Test
    @DisplayName("参加者を保存して全属性を復元できる(任意項目のnullも保てる)")
    void 保存と取得() {
        TournamentId tournamentId = TournamentId.generate();
        Participant withAll = Participant.create("参加 太郎", "A社", Rank.DAN_3, 1, groupId);
        Participant minimal = Participant.create("参加 次郎", null, null, 2, groupId);
        repository.saveAll(tournamentId, List.of(withAll, minimal));

        Participant foundAll = repository.findById(tournamentId, withAll.id()).orElseThrow();
        assertThat(foundAll).isEqualTo(withAll);
        Participant foundMinimal = repository.findById(tournamentId, minimal.id()).orElseThrow();
        assertThat(foundMinimal.organization()).isNull();
        assertThat(foundMinimal.rank()).isNull();
        assertThat(foundMinimal.groupId()).isEqualTo(groupId);
    }

    @Test
    @DisplayName("グループ割当の変更を保存して復元できる")
    void グループ割当の往復() {
        TournamentId tournamentId = TournamentId.generate();
        Participant participant = Participant.create("組分 太郎", null, Rank.DAN_1, 1, groupId);
        repository.save(tournamentId, participant);

        Participant found = repository.findById(tournamentId, participant.id()).orElseThrow();
        assertThat(found.groupId()).isEqualTo(groupId);
        assertThat(found).isEqualTo(participant);

        GroupId moved = GroupId.generate();
        repository.save(tournamentId, found.withGroup(moved));
        assertThat(repository.findById(tournamentId, participant.id()).orElseThrow().groupId())
                .isEqualTo(moved);
    }

    @Test
    @DisplayName("一覧は自大会の参加者のみを返す")
    void 一覧の分離() {
        TournamentId mine = TournamentId.generate();
        TournamentId others = TournamentId.generate();
        repository.save(mine, Participant.create("自分の参加者", null, null, 1, groupId));
        repository.save(others, Participant.create("他大会の参加者", null, null, 1, groupId));

        List<Participant> found = repository.findAllByTournamentId(mine);
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().name()).isEqualTo("自分の参加者");
    }

    @Test
    @DisplayName("更新(棄権)と削除ができる")
    void 更新と削除() {
        TournamentId tournamentId = TournamentId.generate();
        Participant participant = Participant.create("棄権 予定", null, Rank.KYU_5, 1, groupId);
        repository.save(tournamentId, participant);

        repository.save(tournamentId, participant.withdraw());
        assertThat(repository.findById(tournamentId, participant.id()).orElseThrow().status())
                .isEqualTo(ParticipantStatus.WITHDRAWN);

        repository.delete(tournamentId, participant.id());
        assertThat(repository.findById(tournamentId, participant.id())).isEmpty();
    }
}
