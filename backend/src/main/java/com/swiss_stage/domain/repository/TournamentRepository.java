package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Optional;

public interface TournamentRepository {

    Optional<Tournament> findById(TournamentId id);

    List<Tournament> findByOwnerSub(String ownerSub);

    Optional<Tournament> findByShareToken(String shareToken);

    /** versionによる楽観ロック付き保存。競合時は実装が例外を送出する */
    void save(Tournament tournament);

    /** 大会と配下の全データ(参加者・対局)を物理削除する */
    void delete(TournamentId id);
}
