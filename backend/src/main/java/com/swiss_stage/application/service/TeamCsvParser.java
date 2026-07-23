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
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 団体戦チーム+メンバーCSVの解析(03_api_design.md §4-6の団体戦拡張)。
 *
 * <ul>
 *   <li>ヘッダー行必須: {@code チーム名,氏名,段級位,ポジション}(4列)または
 *       {@code チーム名,氏名,段級位,ポジション,グループ}(5列)</li>
 *   <li>連続する同一チーム名の行を1チームとして扱う(チーム名で離れた行を同一チームにはしない)</li>
 *   <li>ポジション列: 主将/副将/三将/四将/五将(teamSize以内)。空欄は補欠</li>
 *   <li>グループ列は任意。同一チームの最初の行の値のみ使う</li>
 *   <li>文字コードは UTF-8 / Shift_JIS を自動判定。行エラーは全行検査してまとめて返す</li>
 * </ul>
 */
@Component
public class TeamCsvParser {

    /** groupName はグループ列なし・空欄なら null。lineNumber は呼び出し側の行エラー報告用 */
    public record Row(
            String teamName, String memberName, Rank rank, Integer boardPosition,
            String groupName, int lineNumber) {}

    private static final int MAX_ROWS = 500;
    private static final String[] BASE_HEADER = {"チーム名", "氏名", "段級位", "ポジション"};
    private static final String GROUP_HEADER = "グループ";
    private static final Charset SHIFT_JIS = Charset.forName("windows-31j");
    private static final Map<String, Integer> POSITION_LABELS = Map.of(
            "主将", 1, "副将", 2, "三将", 3, "四将", 4, "五将", 5);

    public List<Row> parse(byte[] csv, int teamSize) {
        if (csv == null || csv.length == 0) {
            throw invalid(List.of(new FieldErrorDto("file", "CSVファイルが空です")));
        }
        String content = decode(csv);
        List<String> lines = content.lines().toList();
        if (lines.isEmpty()) {
            throw invalid(List.of(new FieldErrorDto("file", "CSVファイルが空です")));
        }
        int columnCount = validateHeader(lines.getFirst());

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
            parseLine(line, i + 1, columnCount, teamSize, rows, errors);
        }
        if (dataRowCount == 0) {
            throw invalid(List.of(new FieldErrorDto("file", "データ行がありません")));
        }
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }
        return rows;
    }

    /** 連続する同一チーム名の行をチームごとにまとめる(離れた行の同名チームは別チーム扱い) */
    public List<List<Row>> groupByTeam(List<Row> rows) {
        List<List<Row>> teams = new ArrayList<>();
        List<Row> current = null;
        String currentName = null;
        for (Row row : rows) {
            if (current == null || !row.teamName().equals(currentName)) {
                current = new ArrayList<>();
                teams.add(current);
                currentName = row.teamName();
            }
            current.add(row);
        }
        return teams;
    }

    private void parseLine(
            String line, int lineNumber, int columnCount, int teamSize,
            List<Row> rows, List<FieldErrorDto> errors) {
        String[] columns = line.split(",", -1);
        String field = lineNumber + "行目";
        if (columns.length != columnCount) {
            errors.add(new FieldErrorDto(field, "列数が不正です(ヘッダーと同じ" + columnCount + "列)"));
            return;
        }
        String teamName = columns[0].strip();
        String memberName = columns[1].strip();
        String rankText = columns[2].strip();
        String positionText = columns[3].strip();
        String groupName = columnCount == 5 ? columns[4].strip() : "";

        if (teamName.isEmpty()) {
            errors.add(new FieldErrorDto(field, "チーム名は必須です"));
            return;
        }
        if (teamName.length() > 50) {
            errors.add(new FieldErrorDto(field, "チーム名は50文字以内で入力してください"));
            return;
        }
        if (memberName.isEmpty()) {
            errors.add(new FieldErrorDto(field, "氏名は必須です"));
            return;
        }
        if (memberName.length() > 50) {
            errors.add(new FieldErrorDto(field, "氏名は50文字以内で入力してください"));
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
        Integer boardPosition = null;
        if (!positionText.isEmpty()) {
            Integer position = POSITION_LABELS.get(positionText);
            if (position == null) {
                errors.add(new FieldErrorDto(field,
                        "ポジション「" + positionText + "」を解釈できません(例: 主将・副将・三将)"));
                return;
            }
            if (position > teamSize) {
                errors.add(new FieldErrorDto(field,
                        "ポジション「" + positionText + "」は" + teamSize + "人制では指定できません"));
                return;
            }
            boardPosition = position;
        }
        rows.add(new Row(
                teamName, memberName, rank, boardPosition,
                groupName.isEmpty() ? null : groupName, lineNumber));
    }

    private int validateHeader(String headerLine) {
        String[] columns = stripBom(headerLine).split(",", -1);
        boolean valid = columns.length == BASE_HEADER.length
                || columns.length == BASE_HEADER.length + 1;
        for (int i = 0; valid && i < BASE_HEADER.length; i++) {
            valid = columns[i].strip().equals(BASE_HEADER[i]);
        }
        if (valid && columns.length == BASE_HEADER.length + 1) {
            valid = columns[BASE_HEADER.length].strip().equals(GROUP_HEADER);
        }
        if (!valid) {
            throw invalid(List.of(new FieldErrorDto(
                    "1行目",
                    "ヘッダー行は「チーム名,氏名,段級位,ポジション」"
                            + "または「チーム名,氏名,段級位,ポジション,グループ」である必要があります")));
        }
        return columns.length;
    }

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
        return line.startsWith("﻿") ? line.substring(1) : line;
    }

    private static ValidationException invalid(List<FieldErrorDto> details) {
        return new ValidationException(
                ErrorCode.CSV_INVALID_FORMAT, "CSVの内容に誤りがあります", details);
    }
}
