package com.swiss_stage.application.service;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Participant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 参加者CSVのダウンロード出力(03_api_design.md §4-9)。 {@link ParticipantCsvParser}
 * と対称のヘッダー(氏名,所属,段級位,グループ)で書き出す。 UTF-8 BOM付き・CRLF区切り(Excel互換)。0件時はヘッダー行のみ返す。
 */
@Component
public class ParticipantCsvWriter {

  private static final String HEADER = "氏名,所属,段級位,グループ\r\n";
  private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

  public byte[] write(List<Participant> participants, Map<GroupId, String> groupNames) {
    StringBuilder sb = new StringBuilder(HEADER);
    for (Participant p : participants) {
      sb.append(p.name())
          .append(',')
          .append(p.organization() == null ? "" : p.organization())
          .append(',')
          .append(p.rank() == null ? "" : p.rank().displayName())
          .append(',')
          .append(groupNames.getOrDefault(p.groupId(), ""))
          .append("\r\n");
    }
    byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
    byte[] out = new byte[BOM.length + body.length];
    System.arraycopy(BOM, 0, out, 0, BOM.length);
    System.arraycopy(body, 0, out, BOM.length, body.length);
    return out;
  }
}
