package com.swiss_stage.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.application.dto.SharedTournamentDto;
import com.swiss_stage.application.service.SharedViewCache;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SharedViewCacheTest {

    private static final String TOKEN = "A".repeat(43);

    private SharedViewCache cache;
    private TournamentId tournamentId;
    private AtomicInteger loadCount;

    @BeforeEach
    void setUp() {
        cache = new SharedViewCache(30);
        tournamentId = TournamentId.generate();
        loadCount = new AtomicInteger();
    }

    private SharedViewCache.Entry load(String token) {
        loadCount.incrementAndGet();
        return new SharedViewCache.Entry(tournamentId, dto("大会" + loadCount.get()));
    }

    private static SharedTournamentDto dto(String name) {
        return new SharedTournamentDto(
                new SharedTournamentDto.SharedTournamentSummary(
                        name, null, null, null, 3, 0, null, false),
                List.of(), List.of());
    }

    @Test
    @DisplayName("2回目以降はローダーを呼ばずキャッシュを返す")
    void キャッシュヒット() {
        SharedTournamentDto first = cache.get(TOKEN, this::load);
        SharedTournamentDto second = cache.get(TOKEN, this::load);
        assertThat(loadCount.get()).isEqualTo(1);
        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("evict後は再ロードされる")
    void evictで再ロード() {
        cache.get(TOKEN, this::load);
        cache.evict(tournamentId);
        SharedTournamentDto reloaded = cache.get(TOKEN, this::load);
        assertThat(loadCount.get()).isEqualTo(2);
        assertThat(reloaded.tournament().name()).isEqualTo("大会2");
    }

    @Test
    @DisplayName("別大会のevictは影響しない")
    void 別大会のevict() {
        cache.get(TOKEN, this::load);
        cache.evict(TournamentId.generate());
        cache.get(TOKEN, this::load);
        assertThat(loadCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("ローダーの例外はキャッシュされず伝播する")
    void 例外はキャッシュしない() {
        assertThatThrownBy(() -> cache.get(TOKEN, t -> {
            throw new IllegalStateException("invalid token");
        })).isInstanceOf(IllegalStateException.class);
        cache.get(TOKEN, this::load);
        assertThat(loadCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("TTL 0なら毎回ロードされる(期限切れの動作確認)")
    void TTL切れ() {
        SharedViewCache shortLived = new SharedViewCache(0);
        shortLived.get(TOKEN, this::load);
        shortLived.get(TOKEN, this::load);
        assertThat(loadCount.get()).isEqualTo(2);
    }
}
