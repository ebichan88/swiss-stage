package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.TournamentId;

final class GroupItemMapper {

    private GroupItemMapper() {}

    static GroupItem toItem(TournamentId tournamentId, Group group) {
        var item = new GroupItem();
        item.setPk(DynamoDbKeys.pk(tournamentId));
        item.setSk(DynamoDbKeys.groupSk(group.id()));
        item.setEntityType(GroupItem.ENTITY_TYPE);
        item.setGroupId(group.id().value());
        item.setName(group.name());
        return item;
    }

    static Group toDomain(GroupItem item) {
        return new Group(new GroupId(item.getGroupId()), item.getName());
    }
}
