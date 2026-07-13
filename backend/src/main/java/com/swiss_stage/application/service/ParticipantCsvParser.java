package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.FieldErrorDto;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.model.Rank;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 参加者CSVの解析(03_api_design.md §4-6)。
 *
 * <ul>
 *   <li>ヘッダー行必須: {@code 氏名,所属,段級位}</li>
 *   <li>文字コードは UTF-8 / Shift_JIS を自動判定(UTF-8として厳密にデコードできなければShift_JIS)</li>
 *   <li>行エラーは全行検査してまとめて返す(1件でもエラーがあれば取り込まない)</li>
 *   <li>行数上限500(13_security_design.md §4)</li>
 * </ul>
 */
@Component
public class ParticipantCsvParser {

    public record Row(String name, String organization, Rank rank) {}

    private static final int MAX_ROWS = 500;
    private static final String[] EXPECTED_HEADER = {"氏名", "所属", "段級位"};
    private static final Charset SHIFT_JIS = Charset.forName("windows-31j");

    public List<Row> parse(byte[] csv) {
        if (csv == null || csv.length == 0) {
            throw invalid(List.of(new FieldErrorDto("file", "CSVファイルが空です")));
        }
        String content = decode(csv);
        List<String> lines = content.lines().toList();
        if (lines.isEmpty()) {
            throw invalid(List.of(new FieldErrorDto("file", "CSVファイルが空です")));
        }
        validateHeader(lines.getFirst());

        List<FieldErrorDto> errors = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        int dataRowCount = 0;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            dataRowCount++;
            if (dataRowCount > MAX_ROWS) {
                throw invalid(List.of(new FieldErrorDto(
                        "file", "データ行が上限(" + MAX_ROWS + "行)を超えています")));
            }
            parseLine(line, i + 1, rows, errors);
        }
        if (dataRowCount == 0) {
            throw invalid(List.of(new FieldErrorDto("file", "データ行がありません")));
        }
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }
        return rows;
    }

    private void parseLine(String line, int lineNumber, List<Row> rows, List<FieldErrorDto> errors) {
        String[] columns = line.split(",", -1);
        String field = lineNumber + "行目";
        if (columns.length != EXPECTED_HEADER.length) {
            errors.add(new FieldErrorDto(field, "列数が不正です(氏名,所属,段級位 の3列)"));
            return;
        }
        String name = columns[0].strip();
        String organization = columns[1].strip();
        String rankText = columns[2].strip();
        if (name.isEmpty()) {
            errors.add(new FieldErrorDto(field, "氏名は必須です"));
            return;
        }
        if (name.length() > 50) {
            errors.add(new FieldErrorDto(field, "氏名は50文字以内で入力してください"));
            return;
        }
        if (organization.length() > 100) {
            errors.add(new FieldErrorDto(field, "所属は100文字以内で入力してください"));
            return;
        }
        Rank rank = null;
        if (!rankText.isEmpty()) {
            Optional<Rank> parsed = Rank.fromDisplayName(rankText);
            if (parsed.isEmpty()) {
                errors.add(new FieldErrorDto(field,
                        "段級位「" + rankText + "」を解釈できません(例: 3級・初段・5段)"));
                return;
            }
            rank = parsed.get();
        }
        rows.add(new Row(name, organization.isEmpty() ? null : organization, rank));
    }

    private void validateHeader(String headerLine) {
        String[] columns = stripBom(headerLine).split(",", -1);
        boolean valid = columns.length == EXPECTED_HEADER.length;
        for (int i = 0; valid && i < EXPECTED_HEADER.length; i++) {
            valid = columns[i].strip().equals(EXPECTED_HEADER[i]);
        }
        if (!valid) {
            throw invalid(List.of(new FieldErrorDto(
                    "1行目", "ヘッダー行は「氏名,所属,段級位」である必要があります")));
        }
    }

    /** UTF-8として厳密にデコードできればUTF-8、できなければShift_JIS(windows-31j)とみなす */
    private String decode(byte[] csv) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(csv))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(csv, SHIFT_JIS);
        }
    }

    private static String stripBom(String line) {
        return line.startsWith("\uFEFF") ? line.substring(1) : line;
    }

    private static ValidationException invalid(List<FieldErrorDto> details) {
        return new ValidationException(
                ErrorCode.CSV_INVALID_FORMAT, "CSVの内容に誤りがあります", details);
    }
}
