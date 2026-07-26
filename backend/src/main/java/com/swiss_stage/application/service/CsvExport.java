package com.swiss_stage.application.service;

/** CSVダウンロードの結果(ファイル名生成用の大会名 + 本文バイト列)。JSON化しないためDTOではなくservice層に置く */
public record CsvExport(String tournamentName, byte[] content) {}
