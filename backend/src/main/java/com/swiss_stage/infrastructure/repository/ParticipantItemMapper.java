package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.TournamentId;

final class ParticipantItemMapper {

    private ParticipantItemMapper() {}

    static ParticipantItem toItem(TournamentId tournamentId, Participant p) {
        var item = new ParticipantItem();
        item.setPk(DynamoDbKeys.pk(tournamentId));
        item.setSk(DynamoDbKeys.participantSk(p.id()));
        item.setEntityType(ParticipantItem.ENTITY_TYPE);
        item.setParticipantId(p.id().value());
        item.setName(p.name());
        item.setOrganization(p.organization());
        item.setRank(p.rank() == null ? null : p.rank().name());
        item.setSeedOrder(p.seedOrder());
        item.setStatus(p.status().name());
        item.setGroupId(p.groupId().value());
        return item;
    }

    static Participant toDomain(ParticipantItem item) {
        return new Participant(
                new ParticipantId(item.getParticipantId()),
                item.getName(),
                item.getOrganization(),
                item.getRank() == null ? null : Rank.valueOf(item.getRank()),
                item.getSeedOrder(),
                ParticipantStatus.valueOf(item.getStatus()),
                new GroupId(item.getGroupId()));
    }
}
