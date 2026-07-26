package com.swiss_stage.presentation.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** CSVダウンロードレスポンスの組み立て。ApiSuccessでラップしない生バイナリレスポンス */
final class CsvDownload {

    private static final String UNSAFE_FILENAME_CHARS = "[\\\\/:*?\"<>|]";

    private CsvDownload() {}

    static ResponseEntity<byte[]> response(String tournamentName, String fileSuffix, byte[] csv) {
        String safeName = tournamentName.replaceAll(UNSAFE_FILENAME_CHARS, "_");
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(safeName + "_" + fileSuffix + ".csv", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }
}
