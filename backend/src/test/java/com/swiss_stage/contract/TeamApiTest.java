package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swiss_stage.application.service.TournamentEntryOrderAllocator;
import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.TeamRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/** 団体戦(competitionType=TEAM)のチーム・メンバー管理APIコントラクト (05_swiss_pairing_algorithm.md §5.1)。 */
class TeamApiTest extends ApiContractTestSupport {

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private TournamentEntryOrderAllocator entryOrderAllocator;

  private String tournamentId;

  @BeforeEach
  void setUp() throws Exception {
    MvcResult result =
        performApi(
                post("/api/v1/tournaments")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"団体戦テスト大会\",\"gameType\":\"GO\","
                            + "\"competitionType\":\"TEAM\",\"teamSize\":3,\"totalRounds\":3}"))
            .andExpect(status().isCreated())
            .andReturn();
    tournamentId = dataOf(result).path("id").asText();
  }

  @Test
  @DisplayName("TEAM-AC-001,TEAM-AC-002: 大会作成時にcompetitionType/teamSizeを指定でき、" + "不正な指定は400になる")
  void 大会作成時のチーム制指定() throws Exception {
    performApi(
            post("/api/v1/tournaments")
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"個人戦にteamSize\",\"gameType\":\"GO\","
                        + "\"competitionType\":\"INDIVIDUAL\",\"teamSize\":3,\"totalRounds\":3}"))
        .andExpect(status().isBadRequest());
    performApi(
            post("/api/v1/tournaments")
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"団体戦teamSizeなし\",\"gameType\":\"GO\","
                        + "\"competitionType\":\"TEAM\",\"totalRounds\":3}"))
        .andExpect(status().isBadRequest());
    // teamSizeはスキーマ上も3/5のみ許容(意図的にスキーマ違反を送るため素のperform)
    mockMvc
        .perform(
            post("/api/v1/tournaments")
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"4チーム制\",\"gameType\":\"GO\","
                        + "\"competitionType\":\"TEAM\",\"teamSize\":4,\"totalRounds\":3}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName(
      "TEAM-AC-003,TEAM-AC-004,TEAM-AC-005: チームを作成するとエントリー順が自動採番され、"
          + "メンバーのboardPosition重複・範囲外は400になる")
  void チーム作成とメンバー追加() throws Exception {
    String teamId = createTeam("Aチーム");

    performApi(
            post(membersPath(teamId))
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"主将 一郎\",\"rank\":\"DAN_3\",\"boardPosition\":1}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.members.length()").value(1))
        .andExpect(jsonPath("$.data.members[0].boardPosition").value(1));

    // 範囲外(3人制で4)は400
    performApi(
            post(membersPath(teamId))
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"範囲外\",\"boardPosition\":4}"))
        .andExpect(status().isBadRequest());

    // 重複(既に1がいる)は400
    performApi(
            post(membersPath(teamId))
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"重複\",\"boardPosition\":1}"))
        .andExpect(status().isBadRequest());

    String secondTeamId = createTeam("Bチーム");
    performApi(get(teamsPath()).cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].entryOrder").value(1))
        .andExpect(jsonPath("$.data[1].entryOrder").value(2));
    org.assertj.core.api.Assertions.assertThat(secondTeamId).isNotBlank();
  }

  @Test
  @DisplayName("TEAM-AC-006: 補欠人数の上限(3チーム制=2名)を超える追加は400になる")
  void 補欠人数の上限() throws Exception {
    String teamId = createTeam("補欠チーム");
    addMember(teamId, "補欠1", null);
    addMember(teamId, "補欠2", null);
    performApi(
            post(membersPath(teamId))
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"補欠3\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("TEAM-AC-007: 必須ポジションが揃っていないチームがあると大会を開始できない")
  void 必須ポジション未充足で開始不可() throws Exception {
    String teamA = createTeam("Aチーム");
    String teamB = createTeam("Bチーム");
    fillRequiredPositions(teamA, 3);
    addMember(teamB, "主将のみ", 1);

    performApi(post(base() + "/start").cookie(ownerCookie()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

    addMember(teamB, "副将", 2);
    addMember(teamB, "三将", 3);
    performApi(post(base() + "/start").cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
  }

  @Test
  @DisplayName("TEAM-AC-008: チーム+メンバー一覧のCSVインポートができ、行エラーは1件も取り込まれない")
  void CSVインポート() throws Exception {
    String csv =
        "チーム名,氏名,段級位,ポジション\n"
            + "Aチーム,主将 一郎,3段,主将\n"
            + "Aチーム,副将 二郎,初段,副将\n"
            + "Aチーム,三将 三郎,,三将\n"
            + "Aチーム,補欠 四郎,5級,\n"
            + "Bチーム,主将 五郎,,主将\n"
            + "Bチーム,副将 六郎,,副将\n"
            + "Bチーム,三将 七郎,,三将\n";
    performApi(
            multipart(teamsPath() + "/csv-import")
                .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                .cookie(ownerCookie()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.importedTeamCount").value(2))
        .andExpect(jsonPath("$.data.teams[0].name").value("Aチーム"))
        .andExpect(jsonPath("$.data.teams[0].members.length()").value(4))
        .andExpect(jsonPath("$.data.teams[1].name").value("Bチーム"))
        .andExpect(jsonPath("$.data.teams[1].members.length()").value(3));

    String badCsv = "チーム名,氏名,段級位,ポジション\nAチーム,一郎,,四将\n";
    performApi(
            multipart(teamsPath() + "/csv-import")
                .file(csvFile(badCsv.getBytes(StandardCharsets.UTF_8)))
                .cookie(ownerCookie()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("CSV_INVALID_FORMAT"));
  }

  @Test
  @DisplayName("TEAM-AC-009: 大会開始後のチーム追加・削除・メンバー構成変更は409になる" + "(棄権(WITHDRAWN)は開始後も可)")
  void 開始後の制約() throws Exception {
    String teamA = createTeam("Aチーム");
    String teamB = createTeam("Bチーム");
    fillRequiredPositions(teamA, 3);
    fillRequiredPositions(teamB, 3);
    performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());

    performApi(
            post(teamsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"遅刻チーム\"}"))
        .andExpect(status().isConflict());
    performApi(delete(teamsPath() + "/" + teamA).cookie(ownerCookie()))
        .andExpect(status().isConflict());
    performApi(
            post(membersPath(teamA))
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"追加補欠\"}"))
        .andExpect(status().isConflict());

    performApi(
            patch(teamsPath() + "/" + teamA)
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"WITHDRAWN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
  }

  @Test
  @DisplayName("TEAM-AC-010,TEAM-AC-011: グループ分けはチーム単位で使え、" + "段級位による自動振り分けは団体戦では非公開になる")
  void グループのチーム対応() throws Exception {
    String teamId = createTeam("Aチーム");
    MvcResult groupResult =
        performApi(
                post(base() + "/groups")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Bグループ\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String groupId = dataOf(groupResult).path("id").asText();

    performApi(
            patch(teamsPath() + "/" + teamId)
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\":\"" + groupId + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.groupId").value(groupId));

    performApi(post(base() + "/groups/auto-assign").cookie(ownerCookie()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
  }

  @Test
  @DisplayName("TEAM-AC-022: チーム一覧CSVダウンロードはCSVインポートと同じ列構成をメンバー1人1行、" + "UTF-8 BOM付きで返す")
  void CSVダウンロード() throws Exception {
    String teamId = createTeam("Aチーム");
    addMember(teamId, "主将 一郎", 1);
    addMember(teamId, "副将 二郎", 2);

    MvcResult result =
        performApi(get(teamsPath() + "/csv-export").cookie(ownerCookie()))
            .andExpect(status().isOk())
            .andReturn();

    assertThat(result.getResponse().getHeader("Content-Type")).contains("text/csv");
    assertThat(result.getResponse().getHeader("Content-Disposition")).contains("attachment");
    assertThat(csvBodyWithoutBom(result))
        .isEqualTo("チーム名,氏名,段級位,ポジション,グループ\r\n" + "Aチーム,主将 一郎,,主将,A\r\n" + "Aチーム,副将 二郎,,副将,A\r\n");
  }

  @Test
  @DisplayName("TEAM-AC-023: チームが0件のときのCSVダウンロードはヘッダー行のみになる")
  void CSVダウンロード0件() throws Exception {
    MvcResult result =
        performApi(get(teamsPath() + "/csv-export").cookie(ownerCookie()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(csvBodyWithoutBom(result)).isEqualTo("チーム名,氏名,段級位,ポジション,グループ\r\n");
  }

  @Test
  @DisplayName("TEAM-AC-024: CSVダウンロードは大会開始後(IN_PROGRESS)でも利用できる")
  void CSVダウンロードは状態を問わない() throws Exception {
    String teamA = createTeam("Aチーム");
    String teamB = createTeam("Bチーム");
    fillRequiredPositions(teamA, 3);
    fillRequiredPositions(teamB, 3);
    performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());

    performApi(get(teamsPath() + "/csv-export").cookie(ownerCookie())).andExpect(status().isOk());
  }

  @Test
  @DisplayName("TEAM-AC-024: メンバーが1人もいないチームはCSVダウンロードの行として出力されない")
  void CSVダウンロードはメンバー0人のチームを出力しない() throws Exception {
    String teamA = createTeam("Aチーム");
    createTeam("空のチーム");
    addMember(teamA, "主将 一郎", 1);

    MvcResult result =
        performApi(get(teamsPath() + "/csv-export").cookie(ownerCookie()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(csvBodyWithoutBom(result))
        .isEqualTo("チーム名,氏名,段級位,ポジション,グループ\r\n" + "Aチーム,主将 一郎,,主将,A\r\n");
  }

  @Test
  @DisplayName("TEAM-AC-026: チームを同時に追加してもentryOrderが重複せず、採番カウンタの競合は409 CONFLICTになる")
  void 同時追加の競合() throws Exception {
    Tournament staleSnapshot =
        tournamentRepository.findById(new TournamentId(tournamentId)).orElseThrow();
    performApi(
            post(teamsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"先に確定\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(1));
    assertThatThrownBy(() -> entryOrderAllocator.allocate(staleSnapshot, 1, () -> 1))
        .isInstanceOf(OptimisticLockException.class);

    int concurrency = 24;
    ExecutorService pool = Executors.newFixedThreadPool(concurrency);
    CyclicBarrier barrier = new CyclicBarrier(concurrency);
    try {
      List<Future<Integer>> futures = new ArrayList<>();
      for (int i = 0; i < concurrency; i++) {
        int index = i;
        futures.add(
            pool.submit(
                () -> {
                  barrier.await();
                  MvcResult result =
                      performApi(
                              post(teamsPath())
                                  .cookie(ownerCookie())
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .content("{\"name\":\"同時追加" + index + "\"}"))
                          .andReturn();
                  int httpStatus = result.getResponse().getStatus();
                  if (httpStatus == 201) {
                    return dataOf(result).path("entryOrder").asInt();
                  }
                  assertThat(httpStatus).isEqualTo(409);
                  return null;
                }));
      }
      List<Integer> entryOrders = new ArrayList<>();
      for (Future<Integer> future : futures) {
        Integer entryOrder = future.get(10, TimeUnit.SECONDS);
        if (entryOrder != null) {
          entryOrders.add(entryOrder);
        }
      }
      assertThat(entryOrders).doesNotHaveDuplicates();
      List<Integer> sorted = entryOrders.stream().sorted().toList();
      assertThat(sorted).isEqualTo(IntStream.rangeClosed(2, 1 + sorted.size()).boxed().toList());
    } finally {
      pool.shutdown();
    }
  }

  @Test
  @DisplayName(
      "TEAM-AC-027: 採番カウンタ未設定の大会(既存大会の移行)でも、"
          + "既存チームの最大entryOrder+1から採番される(0件からの初回追加はentryOrder=1)")
  void 採番カウンタ未設定時の初期化() throws Exception {
    performApi(
            post(teamsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"一チーム目\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(1));

    MvcResult migrated =
        performApi(
                post("/api/v1/tournaments")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"移行団体戦大会\",\"gameType\":\"GO\","
                            + "\"competitionType\":\"TEAM\",\"teamSize\":3,\"totalRounds\":3}"))
            .andExpect(status().isCreated())
            .andReturn();
    String migratedTournamentId = dataOf(migrated).path("id").asText();
    TournamentId migratedId = new TournamentId(migratedTournamentId);
    GroupId groupId = groupRepository.findAllByTournamentId(migratedId).getFirst().id();
    teamRepository.save(migratedId, Team.create("先客チーム", 5, groupId));

    performApi(
            post("/api/v1/tournaments/" + migratedTournamentId + "/teams")
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"後発チーム\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(6));
  }

  @Test
  @DisplayName("TEAM-AC-028: チームCSVインポートは連続したentryOrderの範囲をまとめて確保し、割り込みの追加と重複しない")
  void CSVインポートの範囲確保() throws Exception {
    performApi(
            post(teamsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"先発チーム\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(1));

    String csv = "チーム名,氏名,段級位,ポジション\n" + "Bチーム,主将 一郎,,主将\n" + "Cチーム,主将 二郎,,主将\n";
    performApi(
            multipart(teamsPath() + "/csv-import")
                .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                .cookie(ownerCookie()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.teams[0].entryOrder").value(2))
        .andExpect(jsonPath("$.data.teams[1].entryOrder").value(3));

    performApi(
            post(teamsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"後発チーム\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(4));
  }

  private void fillRequiredPositions(String teamId, int teamSize) throws Exception {
    for (int position = 1; position <= teamSize; position++) {
      addMember(teamId, "メンバー" + position, position);
    }
  }

  private void addMember(String teamId, String name, Integer boardPosition) throws Exception {
    String body =
        boardPosition == null
            ? "{\"name\":\"" + name + "\"}"
            : "{\"name\":\"" + name + "\",\"boardPosition\":" + boardPosition + "}";
    performApi(
            post(membersPath(teamId))
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
  }

  private String createTeam(String name) throws Exception {
    MvcResult result =
        performApi(
                post(teamsPath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode data = dataOf(result);
    return data.path("id").asText();
  }

  private String teamsPath() {
    return base() + "/teams";
  }

  private String membersPath(String teamId) {
    return teamsPath() + "/" + teamId + "/members";
  }

  private String base() {
    return "/api/v1/tournaments/" + tournamentId;
  }

  private static MockMultipartFile csvFile(byte[] bytes) {
    return new MockMultipartFile("file", "teams.csv", "text/csv", bytes);
  }

  private static String csvBodyWithoutBom(MvcResult result) throws Exception {
    byte[] body = result.getResponse().getContentAsByteArray();
    assertThat(body[0] & 0xFF).isEqualTo(0xEF);
    assertThat(body[1] & 0xFF).isEqualTo(0xBB);
    assertThat(body[2] & 0xFF).isEqualTo(0xBF);
    return new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
  }
}
