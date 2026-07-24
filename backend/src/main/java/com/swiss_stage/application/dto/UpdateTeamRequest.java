package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.ParticipantStatus;
import jakarta.validation.constraints.Size;

/** nullの項目は変更しない。groupId は割当先グループの変更(PREPARING中のみ) */
public record UpdateTeamRequest(
        @Size(max = 50, message = "チーム名は50文字以内で入力してください")
        String name,

        String groupId,

        ParticipantStatus status) {}
