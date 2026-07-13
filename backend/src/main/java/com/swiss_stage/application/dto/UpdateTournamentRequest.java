package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Visibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 大会更新(PATCH)。nullの項目は変更しない。versionは楽観ロック用に必須 */
public record UpdateTournamentRequest(
        @Size(max = 100, message = "大会名は100文字以内で入力してください")
        String name,

        Visibility visibility,

        @NotNull(message = "versionは必須です")
        Long version) {}
