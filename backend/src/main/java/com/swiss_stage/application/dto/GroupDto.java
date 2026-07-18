package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Group;

/** 棋力帯グループDTO */
public record GroupDto(String id, String name) {

    public static GroupDto from(Group group) {
        return new GroupDto(group.id().value(), group.name());
    }
}
