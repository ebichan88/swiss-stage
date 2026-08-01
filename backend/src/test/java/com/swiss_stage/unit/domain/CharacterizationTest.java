package com.swiss_stage.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.Rank;
import com.swiss_stage.domain.model.Standing;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamStanding;
import com.swiss_stage.domain.service.PairingOptions;
import com.swiss_stage.domain.service.PairingResult;
import com.swiss_stage.domain.service.StandingCalculator;
import com.swiss_stage.domain.service.SwissPairingService;
import com.swiss_stage.domain.service.TeamStandingCalculator;
import com.swiss_stage.domain.service.TeamSwissPairingService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * マッチング・順位計算の回帰検知テスト(characterization test)。
 *
 * <p>実大会(GAS版)の実データが手元にないため、09_test_strategy.md §3の「ゴールデンテスト」の代替として
 * 現行実装の出力を固定値として記録し、以後の変更で結果が変わらないことを検知する。**仕様の正しさ自体は 保証しない**(実データ入手後にゴールデンテストへ差し替える)。
 *
 * <p>固定入力(固定シードの参加者リスト + 決定論的な勝敗パターン)を SwissPairingService /
 * StandingCalculator(個人戦)・TeamSwissPairingService / TeamStandingCalculator(団体戦)に通し、
 * 各ラウンドのペアリング結果と最終順位表をJSON化して {@code src/test/resources/characterization/} 配下の固定値と比較する。
 *
 * <p>スナップショットの自動生成はしない。ファイルが無ければ「ないので更新フラグを付けて生成してください」と
 * 明確に失敗させる(AIが差分を黙って「更新」で消せる経路を作らないため)。更新は明示フラグ経由に限定する:
 *
 * <pre>./gradlew test --tests "*CharacterizationTest" -Dcharacterization.update=true</pre>
 *
 * <p><b>更新時は差分を必ず人間がレビューすること。</b> ゴールデンテストと違い、このテストは「今の実装が
 * 正しい」ことを保証しない。差分は「意図した仕様変更か、単なる回帰(バグ)か」を人が判断する必要がある。
 */
class CharacterizationTest {

  private static final Path SNAPSHOT_DIR =
      Path.of("src/test/resources/characterization").toAbsolutePath();
  private static final JsonMapper MAPPER =
      JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  @Test
  void 個人戦の固定シナリオが回帰していない() throws IOException {
    // 16名・棋力に差をつけた固定編成(初段〜7級)。randomFirstRound=falseなので
    // 初回ラウンドは棋力順の隣接ペアリングになり乱数は関与しない
    List<Rank> ranks =
        List.of(
            Rank.DAN_1,
            Rank.DAN_1,
            Rank.KYU_1,
            Rank.KYU_1,
            Rank.KYU_3,
            Rank.KYU_3,
            Rank.KYU_5,
            Rank.KYU_5,
            Rank.KYU_7,
            Rank.KYU_7,
            Rank.KYU_9,
            Rank.KYU_9,
            Rank.KYU_11,
            Rank.KYU_11,
            Rank.KYU_13,
            Rank.KYU_13);
    List<Participant> participants = new ArrayList<>();
    for (int i = 0; i < ranks.size(); i++) {
      participants.add(TestData.participant(i + 1, ranks.get(i)));
    }

    SwissPairingService pairingService = new SwissPairingService();
    StandingCalculator standingCalculator = new StandingCalculator();
    PairingOptions options = PairingOptions.defaults();

    List<Match> allMatches = new ArrayList<>();
    Map<String, Object> rounds = new LinkedHashMap<>();
    int totalRounds = 5;

    for (int round = 1; round <= totalRounds; round++) {
      PairingResult result = pairingService.pair(participants, allMatches, round, options);
      rounds.put("round" + round, describePairing(result));

      int table = 1;
      for (PairingResult.Pair pair : result.pairs()) {
        // 決定論的な勝敗: entryOrderが小さい側(卓内でplayer1)が勝つ固定パターン。
        // これにより毎回同じ勝点分布が再現され、SOS/SOSOSの分岐も安定して検証できる
        allMatches.add(
            Match.pairOf(round, table++, pair.player1Id(), pair.player2Id(), TestData.GROUP_ID)
                .withResult(MatchResult.PLAYER1_WIN));
      }
      if (result.hasBye()) {
        allMatches.add(Match.byeOf(round, table, result.byeParticipantId(), TestData.GROUP_ID));
      }
    }

    List<Standing> standings = standingCalculator.calculate(participants, allMatches);

    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("rounds", rounds);
    snapshot.put("finalStandings", describeStandings(standings));

    assertMatchesSnapshot("individual.json", snapshot);
  }

  @Test
  void 団体戦の固定シナリオが回帰していない() throws IOException {
    // 6チーム・3人制の固定編成
    List<Team> teams = TeamTestData.teams(6);

    TeamSwissPairingService pairingService = new TeamSwissPairingService();
    TeamStandingCalculator standingCalculator = new TeamStandingCalculator();

    List<TeamMatch> allMatches = new ArrayList<>();
    Map<String, Object> rounds = new LinkedHashMap<>();
    int totalRounds = 3;

    for (int round = 1; round <= totalRounds; round++) {
      TeamSwissPairingService.PairingResult result = pairingService.pair(teams, allMatches, round);
      rounds.put("round" + round, describeTeamPairing(result));

      for (TeamSwissPairingService.PairingResult.Pair pair : result.pairs()) {
        // team1が全ボード勝ちの固定パターン(teamSize=3)
        allMatches.add(
            TeamTestData.match(
                round,
                teamById(teams, pair.firstId()),
                teamById(teams, pair.secondId()),
                MatchResult.PLAYER1_WIN,
                MatchResult.PLAYER1_WIN,
                MatchResult.PLAYER1_WIN));
      }
      if (result.byeId() != null) {
        allMatches.add(TeamTestData.bye(round, teamById(teams, result.byeId())));
      }
    }

    List<TeamStanding> standings = standingCalculator.calculate(teams, allMatches);

    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("rounds", rounds);
    snapshot.put("finalStandings", describeTeamStandings(standings));

    assertMatchesSnapshot("team.json", snapshot);
  }

  private static Team teamById(List<Team> teams, TeamId id) {
    return teams.stream()
        .filter(t -> t.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("チームが見つかりません: " + id));
  }

  private static List<Map<String, Object>> describePairing(PairingResult result) {
    List<Map<String, Object>> pairs = new ArrayList<>();
    for (PairingResult.Pair pair : result.pairs()) {
      Map<String, Object> p = new LinkedHashMap<>();
      p.put("player1", pair.player1Id().value());
      p.put("player2", pair.player2Id().value());
      pairs.add(p);
    }
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("pairs", pairs);
    m.put("bye", result.hasBye() ? result.byeParticipantId().value() : null);
    m.put("relaxations", result.relaxations().stream().map(Enum::name).sorted().toList());
    return List.of(m);
  }

  private static List<Map<String, Object>> describeTeamPairing(
      TeamSwissPairingService.PairingResult result) {
    List<Map<String, Object>> pairs = new ArrayList<>();
    for (TeamSwissPairingService.PairingResult.Pair pair : result.pairs()) {
      Map<String, Object> p = new LinkedHashMap<>();
      p.put("team1", pair.firstId().value());
      p.put("team2", pair.secondId().value());
      pairs.add(p);
    }
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("pairs", pairs);
    m.put("bye", result.byeId() != null ? result.byeId().value() : null);
    m.put("relaxations", result.relaxations().stream().map(Enum::name).sorted().toList());
    return List.of(m);
  }

  private static List<Map<String, Object>> describeStandings(List<Standing> standings) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Standing s : standings) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("rank", s.rank());
      m.put("participantId", s.participantId().value());
      m.put("wins", s.wins());
      m.put("losses", s.losses());
      m.put("draws", s.draws());
      m.put("points", s.points());
      m.put("sos", s.sos());
      m.put("sosos", s.sosos());
      m.put("hadBye", s.hadBye());
      list.add(m);
    }
    return list;
  }

  private static List<Map<String, Object>> describeTeamStandings(List<TeamStanding> standings) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (TeamStanding s : standings) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("rank", s.rank());
      m.put("teamId", s.teamId().value());
      m.put("wins", s.wins());
      m.put("losses", s.losses());
      m.put("draws", s.draws());
      m.put("points", s.points());
      m.put("sos", s.sos());
      m.put("sosos", s.sosos());
      m.put("hadBye", s.hadBye());
      list.add(m);
    }
    return list;
  }

  private void assertMatchesSnapshot(String fileName, Object actual) throws IOException {
    Path path = SNAPSHOT_DIR.resolve(fileName);
    String actualJson = MAPPER.writeValueAsString(actual);
    boolean update = "true".equals(System.getProperty("characterization.update"));

    if (update) {
      Files.createDirectories(SNAPSHOT_DIR);
      Files.writeString(path, actualJson + System.lineSeparator(), StandardCharsets.UTF_8);
      // 更新モードでは常にpassさせる。差分は git diff で必ず人間がレビューすること
      return;
    }

    if (!Files.exists(path)) {
      fail(
          "スナップショットが存在しません: "
              + path
              + "\n初回は次のコマンドで生成し、内容を必ず人間がレビューしてからコミットしてください:\n"
              + "./gradlew test --tests \"*CharacterizationTest\" -Dcharacterization.update=true");
    }

    String expectedJson = Files.readString(path, StandardCharsets.UTF_8).stripTrailing();
    assertThat(actualJson)
        .as(
            "%s と現在の出力が一致しません。仕様変更が意図的なら" + " -Dcharacterization.update=true で更新し、差分を必ずレビューしてください",
            fileName)
        .isEqualTo(expectedJson);
  }
}
