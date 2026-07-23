package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.GameType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * competitionType=TEAM の場合は teamSize(3または5)が必須。INDIVIDUAL の場合は
 * teamSize を指定できない(schema/openapi.yaml のCreateTournamentRequest参照)。
 * このクロスフィールド制約はBean Validationでは表現しづらいため、application層
 * (TournamentService)でValidationExceptionとして検証する。
 */
public record CreateTournamentRequest(
        @NotBlank(message = "大会名は必須です")
        @Size(max = 100, message = "大会名は100文字以内で入力してください")
        String name,

        @NotNull(message = "競技は必須です")
        GameType gameType,

        @NotNull(message = "大会形式は必須です")
        CompetitionType competitionType,

        Integer teamSize,

        @NotNull(message = "ラウンド数は必須です")
        @Min(value = 1, message = "ラウンド数は1以上で入力してください")
        @Max(value = 8, message = "ラウンド数は8以下で入力してください")
        Integer totalRounds) {}
