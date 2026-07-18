package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Optional;

public interface GroupRepository {

    Optional<Group> findById(TournamentId tournamentId, GroupId id);

    /** 作成順(ULID順)で返す。自動振り分けの割当順もこの順に従う */
    List<Group> findAllByTournamentId(TournamentId tournamentId);

    void save(TournamentId tournamentId, Group group);

    void delete(TournamentId tournamentId, GroupId id);
}
