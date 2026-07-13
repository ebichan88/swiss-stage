package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.ParticipantStatus;
import com.swiss_stage.domain.model.Rank;
import jakarta.validation.constraints.Size;

/** 参加者更新(PATCH)。nullの項目は変更しない。status=WITHDRAWN で途中棄権 */
public record UpdateParticipantRequest(
        @Size(max = 50, message = "氏名は50文字以内で入力してください")
        String name,

        @Size(max = 100, message = "所属は100文字以内で入力してください")
        String organization,

        Rank rank,

        ParticipantStatus status) {}
