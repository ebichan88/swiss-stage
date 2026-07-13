package com.swiss_stage.application.dto;

import java.util.List;

/** CSVインポート結果(全行正常時のみ取り込む) */
public record CsvImportResultDto(int importedCount, List<ParticipantDto> participants) {}
