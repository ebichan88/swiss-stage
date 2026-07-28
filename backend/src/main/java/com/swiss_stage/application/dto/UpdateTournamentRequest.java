package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Visibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 大会更新(PATCH)。nullの項目は変更しない。versionは楽観ロック用に必須。 開催日を未設定に戻す場合は
 * clearEventDate=true(eventDateとの同時指定は400。 UpdateParticipantRequest の clearRank と同じ規約)。
 */
public record UpdateTournamentRequest(
    @Size(max = 100, message = "大会名は100文字以内で入力してください") String name,

    /** 開催日(YYYY-MM-DD)。null = 変更しない */
    LocalDate eventDate,

    /** true で開催日を未設定に戻す */
    Boolean clearEventDate,
    Visibility visibility,

    /** 共有トークン経由の結果入力を許可するか。null = 変更しない */
    Boolean resultInputEnabled,
    @NotNull(message = "versionは必須です") Long version) {}
