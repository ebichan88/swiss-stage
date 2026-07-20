package com.swiss_stage.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.service.GroupAssignmentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GroupAssignmentServiceTest {

    private final GroupAssignmentService service = new GroupAssignmentService();

    private final Group groupA = Group.create("A");
    private final Group groupB = Group.create("B");
    private final Group groupC = Group.create("C");

    @Test
    @DisplayName("自動振り分け: 棋力の強い順にグループ定義順へ均等分割する")
    void 均等分割() {
        List<Participant> participants = List.of(
                TestData.participant(1, Rank.KYU_1),
                TestData.participant(2, Rank.DAN_5),
                TestData.participant(3, Rank.KYU_10),
                TestData.participant(4, Rank.DAN_1),
                TestData.participant(5, Rank.KYU_5),
                TestData.participant(6, Rank.DAN_9));

        Map<ParticipantId, GroupId> assignment =
                service.propose(List.of(groupA, groupB, groupC), participants);

        // 強い順: 9段(6), 5段(2) | 初段(4), 1級(1) | 5級(5), 10級(3)
        assertThat(assignment.get(id(6))).isEqualTo(groupA.id());
        assertThat(assignment.get(id(2))).isEqualTo(groupA.id());
        assertThat(assignment.get(id(4))).isEqualTo(groupB.id());
        assertThat(assignment.get(id(1))).isEqualTo(groupB.id());
        assertThat(assignment.get(id(5))).isEqualTo(groupC.id());
        assertThat(assignment.get(id(3))).isEqualTo(groupC.id());
    }

    @Test
    @DisplayName("自動振り分け: 端数は先頭側のグループに1名ずつ多く割り当てる")
    void 端数は先頭グループへ() {
        List<Participant> participants = TestData.participants(7);

        Map<ParticipantId, GroupId> assignment =
                service.propose(List.of(groupA, groupB, groupC), participants);

        assertThat(countOf(assignment, groupA)).isEqualTo(3);
        assertThat(countOf(assignment, groupB)).isEqualTo(2);
        assertThat(countOf(assignment, groupC)).isEqualTo(2);
    }

    @Test
    @DisplayName("自動振り分け: 棋力未入力者は末尾(最弱側)のグループに入る")
    void 棋力未入力は末尾() {
        List<Participant> participants = List.of(
                TestData.participant(1),
                TestData.participant(2, Rank.KYU_20),
                TestData.participant(3, Rank.DAN_1),
                TestData.participant(4, Rank.KYU_1));

        Map<ParticipantId, GroupId> assignment =
                service.propose(List.of(groupA, groupB), participants);

        // 強い順: 初段(3), 1級(4) | 20級(2), 未入力(1)
        assertThat(assignment.get(id(3))).isEqualTo(groupA.id());
        assertThat(assignment.get(id(4))).isEqualTo(groupA.id());
        assertThat(assignment.get(id(2))).isEqualTo(groupB.id());
        assertThat(assignment.get(id(1))).isEqualTo(groupB.id());
    }

    @Test
    @DisplayName("自動振り分け: 棄権者(WITHDRAWN)は割当対象外")
    void 棄権者は対象外() {
        Participant withdrawn = TestData.participant(1, Rank.DAN_9).withdraw();
        List<Participant> participants = List.of(withdrawn, TestData.participant(2), TestData.participant(3));

        Map<ParticipantId, GroupId> assignment =
                service.propose(List.of(groupA), participants);

        assertThat(assignment).doesNotContainKey(withdrawn.id());
        assertThat(assignment).hasSize(2);
    }

    @Test
    @DisplayName("自動振り分け: グループ未定義ならエラー")
    void グループ未定義はエラー() {
        assertThatThrownBy(() -> service.propose(List.of(), TestData.participants(4)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("開始時検証: グループ未定義はエラー(大会は常に1つ以上のグループを持つ)")
    void 検証はグループ未定義でエラー() {
        assertThatThrownBy(() -> service.validateForStart(List.of(), TestData.participants(4)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("開始時検証: 全ACTIVE参加者が割当済みで各グループ2名以上なら通る")
    void 検証OK() {
        List<Participant> participants = List.of(
                TestData.participant(1).withGroup(groupA.id()),
                TestData.participant(2).withGroup(groupA.id()),
                TestData.participant(3).withGroup(groupB.id()),
                TestData.participant(4).withGroup(groupB.id()),
                // 棄権者は割当先が定義外グループでも数に入らない
                TestData.participant(5).withdraw());

        assertThatCode(() -> service.validateForStart(List.of(groupA, groupB), participants))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("開始時検証: 2名未満のグループがあると開始できない")
    void 二名未満はエラー() {
        List<Participant> participants = List.of(
                TestData.participant(1).withGroup(groupA.id()),
                TestData.participant(2).withGroup(groupA.id()),
                TestData.participant(3).withGroup(groupB.id()));

        assertThatThrownBy(() -> service.validateForStart(List.of(groupA, groupB), participants))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("B");
    }

    @Test
    @DisplayName("開始時検証: 存在しないグループへの割当は未割当扱い")
    void 存在しないグループはエラー() {
        List<Participant> participants = List.of(
                TestData.participant(1).withGroup(groupA.id()),
                TestData.participant(2).withGroup(groupA.id()),
                TestData.participant(3).withGroup(GroupId.generate()));

        assertThatThrownBy(() -> service.validateForStart(List.of(groupA), participants))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("未割当");
    }

    private static ParticipantId id(int entryOrder) {
        return TestData.participant(entryOrder).id();
    }

    private static long countOf(Map<ParticipantId, GroupId> assignment, Group group) {
        return assignment.values().stream().filter(group.id()::equals).count();
    }
}
