package com.swiss_stage.unit.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamMember;
import com.swiss_stage.domain.service.TeamRosterValidationService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamRosterValidationServiceTest {

    private final TeamRosterValidationService service = new TeamRosterValidationService();

    @Test
    @DisplayName("3チーム制は補欠2名まで、5チーム制は補欠3名まで")
    void 補欠人数の上限() {
        assertThatCode(() -> service.validateReserveCount(teamWithReserves(2), 3))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateReserveCount(teamWithReserves(3), 3))
                .isInstanceOf(DomainException.class);
        assertThatCode(() -> service.validateReserveCount(teamWithReserves(3), 5))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateReserveCount(teamWithReserves(4), 5))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("開始時検証: 必須ポジション(1..teamSize)が過不足なく埋まっていれば通る")
    void 開始時検証OK() {
        Team team = TeamTestData.team(1)
                .withMember(TeamMember.create("主将", null, 1))
                .withMember(TeamMember.create("副将", null, 2))
                .withMember(TeamMember.create("三将", null, 3));

        assertThatCode(() -> service.validateForStart(List.of(team), 3))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("開始時検証: 必須ポジションが1つでも欠けていると開始できない")
    void 必須ポジション欠けはエラー() {
        Team team = TeamTestData.team(1)
                .withMember(TeamMember.create("主将", null, 1))
                .withMember(TeamMember.create("三将", null, 3));

        assertThatThrownBy(() -> service.validateForStart(List.of(team), 3))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining(team.name());
    }

    @Test
    @DisplayName("開始時検証: 棄権(WITHDRAWN)チームは検証対象外")
    void 棄権チームは検証対象外() {
        Team incomplete = TeamTestData.team(1)
                .withMember(TeamMember.create("主将", null, 1))
                .withdraw();

        assertThatCode(() -> service.validateForStart(List.of(incomplete), 3))
                .doesNotThrowAnyException();
    }

    private static Team teamWithReserves(int reserveCount) {
        Team team = TeamTestData.team(1);
        for (int i = 0; i < reserveCount; i++) {
            team = team.withMember(TeamMember.create("補欠" + i, null, null));
        }
        return team;
    }
}
