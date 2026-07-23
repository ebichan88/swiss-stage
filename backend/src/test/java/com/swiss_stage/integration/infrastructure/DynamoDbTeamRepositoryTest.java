package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamMember;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.TeamRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbTeamRepositoryTest extends DynamoDbRepositoryTestSupport {

    @Autowired TeamRepository repository;

    private final GroupId groupId = GroupId.generate();

    @Test
    @DisplayName("チームを保存すると埋め込みメンバー一覧(役割・棋力込み)も復元できる")
    void 保存と取得() {
        TournamentId tournamentId = TournamentId.generate();
        Team team = Team.create("Aチーム", 1, groupId)
                .withMember(TeamMember.create("主将 一郎", Rank.DAN_3, 1))
                .withMember(TeamMember.create("副将 二郎", Rank.DAN_1, 2))
                .withMember(TeamMember.create("三将 三郎", null, 3))
                .withMember(TeamMember.create("補欠 四郎", Rank.KYU_5, null));
        repository.save(tournamentId, team);

        Team found = repository.findById(tournamentId, team.id()).orElseThrow();
        assertThat(found.name()).isEqualTo("Aチーム");
        assertThat(found.entryOrder()).isEqualTo(1);
        assertThat(found.status()).isEqualTo(ParticipantStatus.ACTIVE);
        assertThat(found.groupId()).isEqualTo(groupId);
        assertThat(found.members()).hasSize(4);
        assertThat(found.members().get(0).name()).isEqualTo("主将 一郎");
        assertThat(found.members().get(0).rank()).isEqualTo(Rank.DAN_3);
        assertThat(found.members().get(0).boardPosition()).isEqualTo(1);
        assertThat(found.members().get(2).rank()).isNull();
        assertThat(found.members().get(3).isReserve()).isTrue();
        assertThat(found.reserveCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("メンバーがいないチーム(登録直後)も保存・復元できる")
    void メンバーなしの保存() {
        TournamentId tournamentId = TournamentId.generate();
        Team team = Team.create("新規チーム", 1, groupId);
        repository.save(tournamentId, team);

        Team found = repository.findById(tournamentId, team.id()).orElseThrow();
        assertThat(found.members()).isEmpty();
    }

    @Test
    @DisplayName("一覧は自大会のチームのみを返し、棄権・グループ変更を保存・復元できる")
    void 一覧の分離と状態更新() {
        TournamentId mine = TournamentId.generate();
        TournamentId others = TournamentId.generate();
        Team myTeam = Team.create("自分の大会のチーム", 1, groupId);
        repository.save(mine, myTeam);
        repository.save(others, Team.create("他大会のチーム", 1, groupId));

        List<Team> found = repository.findAllByTournamentId(mine);
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().name()).isEqualTo("自分の大会のチーム");

        GroupId moved = GroupId.generate();
        repository.save(mine, myTeam.withGroup(moved).withdraw());
        Team updated = repository.findById(mine, myTeam.id()).orElseThrow();
        assertThat(updated.groupId()).isEqualTo(moved);
        assertThat(updated.status()).isEqualTo(ParticipantStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("削除ができる")
    void 削除() {
        TournamentId tournamentId = TournamentId.generate();
        Team team = Team.create("削除対象チーム", 1, groupId);
        repository.save(tournamentId, team);

        repository.delete(tournamentId, team.id());
        assertThat(repository.findById(tournamentId, team.id())).isEmpty();
    }
}
