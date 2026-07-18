package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.GroupRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbGroupRepositoryTest extends DynamoDbRepositoryTestSupport {

    @Autowired GroupRepository repository;

    @Test
    @DisplayName("グループを保存して復元できる")
    void 保存と取得() {
        TournamentId tournamentId = TournamentId.generate();
        Group group = Group.create("A");
        repository.save(tournamentId, group);

        assertThat(repository.findById(tournamentId, group.id())).contains(group);
    }

    @Test
    @DisplayName("一覧は自大会のグループのみを作成順(ULID順)で返す")
    void 一覧の分離と順序() {
        TournamentId mine = TournamentId.generate();
        TournamentId others = TournamentId.generate();
        Group first = Group.create("A");
        Group second = Group.create("B");
        Group third = Group.create("C");
        repository.save(mine, second);
        repository.save(mine, third);
        repository.save(mine, first);
        repository.save(others, Group.create("他大会"));

        List<Group> found = repository.findAllByTournamentId(mine);
        // ULIDは生成順に昇順なので、保存順によらず作成順で返る
        assertThat(found).containsExactly(first, second, third);
    }

    @Test
    @DisplayName("改名と削除ができる")
    void 更新と削除() {
        TournamentId tournamentId = TournamentId.generate();
        Group group = Group.create("A");
        repository.save(tournamentId, group);

        repository.save(tournamentId, group.rename("Aクラス"));
        assertThat(repository.findById(tournamentId, group.id()).orElseThrow().name())
                .isEqualTo("Aクラス");

        repository.delete(tournamentId, group.id());
        assertThat(repository.findById(tournamentId, group.id())).isEmpty();
    }
}
