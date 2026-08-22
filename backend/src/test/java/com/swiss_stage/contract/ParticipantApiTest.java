package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swiss_stage.application.service.TournamentEntryOrderAllocator;
import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.nio.charset.Charset;
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

class ParticipantApiTest extends ApiContractTestSupport {

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private ParticipantRepository participantRepository;
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
                        "{\"name\":\"参加者テスト大会\",\"gameType\":\"GO\",\"competitionType\":\"INDIVIDUAL\",\"totalRounds\":3}"))
            .andExpect(status().isCreated())
            .andReturn();
    tournamentId = dataOf(result).path("id").asText();
  }

  @Test
  @DisplayName("PTC-AC-001: 参加者を追加するとエントリー順が自動採番される")
  void 追加() throws Exception {
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"一人目\",\"organization\":\"A社\",\"rank\":\"DAN_3\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(1))
        .andExpect(jsonPath("$.data.rank").value("DAN_3"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"二人目\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(2))
        .andExpect(jsonPath("$.data.organization").doesNotExist());

    performApi(get(participantsPath()).cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2));
  }

  @Test
  @DisplayName("PTC-AC-002: CSVインポート(UTF-8)で全行取り込める")
  void CSVインポートUTF8() throws Exception {
    String csv = "氏名,所属,段級位\n蛯名 隆,〇〇株式会社,3級\n山田 花子,,初段\n佐藤 一,B社,\n";
    performApi(
            multipart(participantsPath() + "/import")
                .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                .cookie(ownerCookie()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.importedCount").value(3))
        .andExpect(jsonPath("$.data.participants[0].name").value("蛯名 隆"))
        .andExpect(jsonPath("$.data.participants[0].rank").value("KYU_3"))
        .andExpect(jsonPath("$.data.participants[1].rank").value("DAN_1"))
        .andExpect(jsonPath("$.data.participants[2].rank").doesNotExist());
  }

  @Test
  @DisplayName("PTC-AC-003: CSVインポート(Shift_JIS)も自動判定して取り込める")
  void CSVインポートShiftJIS() throws Exception {
    String csv = "氏名,所属,段級位\n蛯名 隆,囲碁部,5段\n";
    performApi(
            multipart(participantsPath() + "/import")
                .file(csvFile(csv.getBytes(Charset.forName("windows-31j"))))
                .cookie(ownerCookie()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.importedCount").value(1))
        .andExpect(jsonPath("$.data.participants[0].name").value("蛯名 隆"))
        .andExpect(jsonPath("$.data.participants[0].organization").value("囲碁部"))
        .andExpect(jsonPath("$.data.participants[0].rank").value("DAN_5"));
  }

  @Test
  @DisplayName("PTC-AC-004: CSVの行エラーは行番号付きdetailsで400になり、1件も取り込まれない")
  void CSVインポート行エラー() throws Exception {
    String csv = "氏名,所属,段級位\n,A社,3級\n正常 太郎,B社,初段\n異常 次郎,C社,超段\n";
    performApi(
            multipart(participantsPath() + "/import")
                .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                .cookie(ownerCookie()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("CSV_INVALID_FORMAT"))
        .andExpect(jsonPath("$.error.details.length()").value(2))
        .andExpect(jsonPath("$.error.details[0].field").value("2行目"))
        .andExpect(jsonPath("$.error.details[1].field").value("4行目"));

    performApi(get(participantsPath()).cookie(ownerCookie()))
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  @Test
  @DisplayName("PTC-AC-005: ヘッダー行が不正なCSVは400になる")
  void CSVヘッダー不正() throws Exception {
    performApi(
            multipart(participantsPath() + "/import")
                .file(csvFile("name,org,rank\nx,y,z\n".getBytes(StandardCharsets.UTF_8)))
                .cookie(ownerCookie()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("CSV_INVALID_FORMAT"))
        .andExpect(jsonPath("$.error.details[0].field").value("1行目"));
  }

  @Test
  @DisplayName("PTC-AC-006,PTC-AC-007: 棄権(PATCH)はいつでもでき、大会開始後の追加・削除は409になる")
  void 開始後の制約と棄権() throws Exception {
    MvcResult p1 =
        performApi(
                post(participantsPath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"参加 一郎\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"参加 二郎\"}"))
        .andExpect(status().isCreated());
    String participantId = dataOf(p1).path("id").asText();

    performApi(post("/api/v1/tournaments/" + tournamentId + "/start").cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"遅刻 三郎\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

    performApi(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    participantsPath() + "/" + participantId)
                .cookie(ownerCookie()))
        .andExpect(status().isConflict());

    performApi(
            patch(participantsPath() + "/" + participantId)
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"WITHDRAWN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
  }

  @Test
  @DisplayName("PTC-AC-008,PTC-AC-009: clearRank=trueで棋力を未入力に戻せ(rankとの同時指定は400)、未指定項目は失われない")
  void 棋力のクリア() throws Exception {
    MvcResult created =
        performApi(
                post(participantsPath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"棋力 未定\",\"rank\":\"DAN_3\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String participantId = dataOf(created).path("id").asText();

    String groupId = dataOf(created).path("groupId").asText();
    performApi(
            patch(participantsPath() + "/" + participantId)
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clearRank\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.rank").doesNotExist())
        .andExpect(jsonPath("$.data.name").value("棋力 未定"))
        // グループ割当は他項目の更新で失われない
        .andExpect(jsonPath("$.data.groupId").value(groupId));

    performApi(
            patch(participantsPath() + "/" + participantId)
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rank\":\"KYU_1\",\"clearRank\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  @DisplayName("PTC-AC-010: 参加者一覧CSVダウンロードはCSVインポートと同じ列構成をUTF-8 BOM付きで返す")
  void CSVダウンロード() throws Exception {
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"蛯名 隆\",\"organization\":\"〇〇株式会社\",\"rank\":\"KYU_3\"}"))
        .andExpect(status().isCreated());
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"山田 花子\",\"rank\":\"DAN_1\"}"))
        .andExpect(status().isCreated());

    MvcResult result =
        performApi(get(participantsPath() + "/export").cookie(ownerCookie()))
            .andExpect(status().isOk())
            .andReturn();

    assertThat(result.getResponse().getHeader("Content-Type")).contains("text/csv");
    assertThat(result.getResponse().getHeader("Content-Disposition")).contains("attachment");
    assertThat(csvBodyWithoutBom(result))
        .isEqualTo("氏名,所属,段級位,グループ\r\n" + "蛯名 隆,〇〇株式会社,3級,A\r\n" + "山田 花子,,初段,A\r\n");
  }

  @Test
  @DisplayName("PTC-AC-011: 参加者が0件のときのCSVダウンロードはヘッダー行のみになる")
  void CSVダウンロード0件() throws Exception {
    MvcResult result =
        performApi(get(participantsPath() + "/export").cookie(ownerCookie()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(csvBodyWithoutBom(result)).isEqualTo("氏名,所属,段級位,グループ\r\n");
  }

  @Test
  @DisplayName("PTC-AC-012: CSVダウンロードは大会開始後(IN_PROGRESS)でも利用できる")
  void CSVダウンロードは状態を問わない() throws Exception {
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"参加 一郎\"}"))
        .andExpect(status().isCreated());
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"参加 二郎\"}"))
        .andExpect(status().isCreated());

    performApi(post("/api/v1/tournaments/" + tournamentId + "/start").cookie(ownerCookie()))
        .andExpect(status().isOk());
    performApi(get(participantsPath() + "/export").cookie(ownerCookie()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("CSVダウンロード結果はそのまま別の大会にインポートでき往復できる")
  void CSVダウンロードと再インポートの往復() throws Exception {
    String csv = "氏名,所属,段級位\n蛯名 隆,〇〇株式会社,3級\n山田 花子,,初段\n";
    performApi(
            multipart(participantsPath() + "/import")
                .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                .cookie(ownerCookie()))
        .andExpect(status().isCreated());

    MvcResult exported =
        performApi(get(participantsPath() + "/export").cookie(ownerCookie()))
            .andExpect(status().isOk())
            .andReturn();
    byte[] exportedCsv = exported.getResponse().getContentAsByteArray();

    MvcResult newTournament =
        performApi(
                post("/api/v1/tournaments")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"流用先大会\",\"gameType\":\"GO\","
                            + "\"competitionType\":\"INDIVIDUAL\",\"totalRounds\":3}"))
            .andExpect(status().isCreated())
            .andReturn();
    String newTournamentId = dataOf(newTournament).path("id").asText();

    performApi(
            multipart("/api/v1/tournaments/" + newTournamentId + "/participants/import")
                .file(csvFile(exportedCsv))
                .cookie(ownerCookie()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.importedCount").value(2))
        .andExpect(jsonPath("$.data.participants[0].name").value("蛯名 隆"))
        .andExpect(jsonPath("$.data.participants[0].organization").value("〇〇株式会社"))
        .andExpect(jsonPath("$.data.participants[0].rank").value("KYU_3"))
        .andExpect(jsonPath("$.data.participants[1].name").value("山田 花子"))
        .andExpect(jsonPath("$.data.participants[1].rank").value("DAN_1"));
  }

  @Test
  @DisplayName("PTC-AC-014: 参加者を同時に追加してもentryOrderが重複せず、採番カウンタの競合は409 CONFLICTになる")
  void 同時追加の競合() throws Exception {
    // 運営者Aがこれから使うつもりで読み込んだ状態(採番カウンタ未初期化)を、
    // 運営者Bの追加が確定した後に保存しようとすると、同じカウンタへの競合として409相当の
    // OptimisticLockExceptionになる(GlobalExceptionHandlerがCONFLICTへ変換する経路は
    // TournamentApiTest/RoundApiTestの楽観ロックテストで別途検証済みのため、ここでは
    // TournamentEntryOrderAllocator自体が競合を検出することを直接確認する)
    Tournament staleSnapshot =
        tournamentRepository.findById(new TournamentId(tournamentId)).orElseThrow();
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"先に確定\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(1));
    assertThatThrownBy(() -> entryOrderAllocator.allocate(staleSnapshot, 1, () -> 1))
        .isInstanceOf(OptimisticLockException.class);

    // 実際に多数の運営者が同時にリクエストしても、entryOrderの重複・欠落が起きないことを確認する
    // (真の競合が実際に発生するかはスレッドスケジューリング依存のため、409の発生自体は断定しない。
    // 断定できるのは「成功したレスポンスのentryOrderが重複しない」という安全性のほうである)
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
                              post(participantsPath())
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
      // 既に1件(先に確定)があるため、2から連番で欠落なく続く
      assertThat(sorted).isEqualTo(IntStream.rangeClosed(2, 1 + sorted.size()).boxed().toList());
    } finally {
      pool.shutdown();
    }
  }

  @Test
  @DisplayName(
      "PTC-AC-015: 採番カウンタ未設定の大会(既存大会の移行)でも、"
          + "既存参加者の最大entryOrder+1から採番される(0人からの初回追加はentryOrder=1)")
  void 採番カウンタ未設定時の初期化() throws Exception {
    // 0人からの初回追加はentryOrder=1(採番カウンタ未初期化の新規大会)
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"一人目\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(1));

    // 既存大会の移行を模して、別の大会で採番カウンタを経由せずentryOrder=5の参加者を用意する
    MvcResult migrated =
        performApi(
                post("/api/v1/tournaments")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"移行大会\",\"gameType\":\"GO\","
                            + "\"competitionType\":\"INDIVIDUAL\",\"totalRounds\":3}"))
            .andExpect(status().isCreated())
            .andReturn();
    String migratedTournamentId = dataOf(migrated).path("id").asText();
    TournamentId migratedId = new TournamentId(migratedTournamentId);
    GroupId groupId = groupRepository.findAllByTournamentId(migratedId).getFirst().id();
    participantRepository.save(migratedId, Participant.create("先客", null, null, 5, groupId));

    performApi(
            post("/api/v1/tournaments/" + migratedTournamentId + "/participants")
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"後発\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(6));
  }

  @Test
  @DisplayName("PTC-AC-016: CSVインポートは連続したentryOrderの範囲をまとめて確保し、割り込みの追加と重複しない")
  void CSVインポートの範囲確保() throws Exception {
    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"先発\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(1));

    String csv = "氏名,所属,段級位\n二人目,,\n三人目,,\n四人目,,\n";
    performApi(
            multipart(participantsPath() + "/import")
                .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                .cookie(ownerCookie()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.participants[0].entryOrder").value(2))
        .andExpect(jsonPath("$.data.participants[1].entryOrder").value(3))
        .andExpect(jsonPath("$.data.participants[2].entryOrder").value(4));

    performApi(
            post(participantsPath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"後発\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.entryOrder").value(5));
  }

  private String participantsPath() {
    return "/api/v1/tournaments/" + tournamentId + "/participants";
  }

  private static MockMultipartFile csvFile(byte[] bytes) {
    return new MockMultipartFile("file", "participants.csv", "text/csv", bytes);
  }

  private static String csvBodyWithoutBom(MvcResult result) throws Exception {
    byte[] body = result.getResponse().getContentAsByteArray();
    assertThat(body[0] & 0xFF).isEqualTo(0xEF);
    assertThat(body[1] & 0xFF).isEqualTo(0xBB);
    assertThat(body[2] & 0xFF).isEqualTo(0xBF);
    return new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
  }
}
