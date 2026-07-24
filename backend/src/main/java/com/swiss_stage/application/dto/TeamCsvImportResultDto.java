package com.swiss_stage.application.dto;

import java.util.List;

/** チームCSVインポート結果(全行正常時のみ取り込む) */
public record TeamCsvImportResultDto(int importedTeamCount, List<TeamDto> teams) {}
