package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Team;

/** 組み合わせ・戦績一覧・順位表で使う表示用の要約。メンバーの個人名は含めない */
public record TeamSummaryDto(String id, String name, int entryOrder) {

    public static TeamSummaryDto from(Team t) {
        return new TeamSummaryDto(t.id().value(), t.name(), t.entryOrder());
    }
}
