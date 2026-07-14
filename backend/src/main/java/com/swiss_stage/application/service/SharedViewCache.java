package com.swiss_stage.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiss_stage.application.dto.SharedTournamentDto;
import com.swiss_stage.domain.model.TournamentId;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 共有ページ集約(GET /api/v1/shared/{token})のインメモリキャッシュ
 * (14_performance_optimization.md §3)。ラウンド確定直後に参加者全員(最大300名)が
 * 一斉に組み合わせ・順位表を見るスパイクを、DynamoDBへのQuery数回に抑えて吸収する。
 *
 * <p>整合性は「更新系操作での明示evict」で保ち、TTLは取りこぼしの安全網。
 * evictとgetの競合では古い値が最長TTLの間残り得るが、単一インスタンス・
 * 30秒TTL前提で許容する(結果の正しさはDynamoDBが常に真)。
 *
 * <p>ローダーの例外(無効トークン等)はキャッシュされず、そのまま伝播する。
 */
@Component
public class SharedViewCache {

    /** token → 集約DTO。同一キーの同時ロードはCaffeineが1回に絞る */
    private final Cache<String, Entry> byToken;
    /** evict用の逆引き(大会 → 現在キャッシュされているtoken)。大会のtokenは常に1つ */
    private final ConcurrentHashMap<TournamentId, String> tokenIndex = new ConcurrentHashMap<>();

    public record Entry(TournamentId tournamentId, SharedTournamentDto dto) {}

    public SharedViewCache(@Value("${app.cache.shared.ttl-seconds:30}") long ttlSeconds) {
        this.byToken = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(1_000)
                .build();
    }

    /** キャッシュがあれば返し、なければloaderで組み立ててキャッシュする */
    public SharedTournamentDto get(String token, Function<String, Entry> loader) {
        Entry entry = byToken.get(token, loader);
        tokenIndex.put(entry.tournamentId(), token);
        return entry.dto();
    }

    /** 大会に変更があったら呼ぶ(結果入力・ラウンド生成/確定・大会/参加者更新・トークン再発行) */
    public void evict(TournamentId tournamentId) {
        String token = tokenIndex.remove(tournamentId);
        if (token != null) {
            byToken.invalidate(token);
        }
    }
}
