package com.swiss_stage.application.service;

import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamMember;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 団体戦チーム+メンバーCSVのダウンロード出力(03_api_design.md §4-9)。 {@link TeamCsvParser}
 * と対称のヘッダー(チーム名,氏名,段級位,ポジション,グループ)で メンバー1人につき1行書き出す。UTF-8 BOM付き・CRLF区切り(Excel互換)。0件時はヘッダー行のみ返す。
 * メンバーが1人もいないチームは行として表現できないため出力されない(既知の制約)。
 */
@Component
public class TeamCsvWriter {

  private static final String HEADER = "チーム名,氏名,段級位,ポジション,グループ\r\n";
  private static final Map<Integer, String> POSITION_NAMES =
      Map.of(1, "主将", 2, "副将", 3, "三将", 4, "四将", 5, "五将");
  private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

  public byte[] write(List<Team> teams, Map<GroupId, String> groupNames) {
    StringBuilder sb = new StringBuilder(HEADER);
    for (Team team : teams) {
      String groupName = groupNames.getOrDefault(team.groupId(), "");
      for (TeamMember member : team.members()) {
        sb.append(team.name())
            .append(',')
            .append(member.name())
            .append(',')
            .append(member.rank() == null ? "" : member.rank().displayName())
            .append(',')
            .append(
                member.boardPosition() == null ? "" : POSITION_NAMES.get(member.boardPosition()))
            .append(',')
            .append(groupName)
            .append("\r\n");
      }
    }
    byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
    byte[] out = new byte[BOM.length + body.length];
    System.arraycopy(BOM, 0, out, 0, BOM.length);
    System.arraycopy(body, 0, out, BOM.length, body.length);
    return out;
  }
}
