package com.swiss_stage.contract;

import com.swiss_stage.domain.DuplicateRoundException;
import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamMatchId;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.MatchRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.repository.RoundRepository;
import com.swiss_stage.domain.repository.TeamMatchRepository;
import com.swiss_stage.domain.repository.TeamRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * APIコントラクトテスト用のインメモリリポジトリ(09_test_strategy.md: presentation層はリポジトリをモック)。
 * 楽観ロック(versionの検証とインクリメント)と二重生成防止はDynamoDB実装と同じ振る舞いを再現する。
 */
@TestConfiguration
public class InMemoryRepositoryConfig {

    @Bean
    @Primary
    public TournamentRepository inMemoryTournamentRepository() {
        return new TournamentRepository() {
            private final Map<String, Tournament> store = new ConcurrentHashMap<>();

            @Override
            public Optional<Tournament> findById(TournamentId id) {
                return Optional.ofNullable(store.get(id.value()));
            }

            @Override
            public List<Tournament> findByOwnerSub(String ownerSub) {
                return store.values().stream()
                        .filter(t -> t.ownerSub().equals(ownerSub))
                        .sorted(Comparator.comparing(Tournament::createdAt).reversed())
                        .toList();
            }

            @Override
            public Optional<Tournament> findByShareToken(String shareToken) {
                return store.values().stream()
                        .filter(t -> shareToken.equals(t.shareToken()))
                        .findFirst();
            }

            @Override
            public void save(Tournament tournament) {
                Tournament stored = store.get(tournament.id().value());
                long storedVersion = stored == null ? 0 : stored.version();
                if (tournament.version() != storedVersion) {
                    throw new OptimisticLockException("大会が他の操作で更新されています");
                }
                store.put(tournament.id().value(), withVersion(tournament, storedVersion + 1));
            }

            @Override
            public void delete(TournamentId id) {
                store.remove(id.value());
            }

            private Tournament withVersion(Tournament t, long version) {
                return new Tournament(t.id(), t.name(), t.gameType(), t.competitionType(),
                        t.teamSize(), t.totalRounds(), t.currentRound(), t.status(), t.visibility(),
                        t.shareToken(), t.resultInputEnabled(), t.ownerSub(), version, t.createdAt(),
                        t.updatedAt());
            }
        };
    }

    @Bean
    @Primary
    public ParticipantRepository inMemoryParticipantRepository() {
        return new ParticipantRepository() {
            private final Map<String, Map<String, Participant>> store = new ConcurrentHashMap<>();

            @Override
            public Optional<Participant> findById(TournamentId tournamentId, ParticipantId id) {
                return Optional.ofNullable(byTournament(tournamentId).get(id.value()));
            }

            @Override
            public List<Participant> findAllByTournamentId(TournamentId tournamentId) {
                return List.copyOf(byTournament(tournamentId).values());
            }

            @Override
            public void save(TournamentId tournamentId, Participant participant) {
                byTournament(tournamentId).put(participant.id().value(), participant);
            }

            @Override
            public void saveAll(TournamentId tournamentId, List<Participant> participants) {
                participants.forEach(p -> save(tournamentId, p));
            }

            @Override
            public void delete(TournamentId tournamentId, ParticipantId id) {
                byTournament(tournamentId).remove(id.value());
            }

            private Map<String, Participant> byTournament(TournamentId tournamentId) {
                return store.computeIfAbsent(tournamentId.value(), k -> new ConcurrentHashMap<>());
            }
        };
    }

    @Bean
    @Primary
    public RoundRepository inMemoryRoundRepository() {
        return new RoundRepository() {
            private final Map<String, Map<Integer, Round>> store = new ConcurrentHashMap<>();

            @Override
            public Optional<Round> findByRoundNumber(TournamentId tournamentId, int roundNumber) {
                return Optional.ofNullable(byTournament(tournamentId).get(roundNumber));
            }

            @Override
            public List<Round> findAllByTournamentId(TournamentId tournamentId) {
                return byTournament(tournamentId).values().stream()
                        .sorted(Comparator.comparingInt(Round::roundNumber))
                        .toList();
            }

            @Override
            public void create(TournamentId tournamentId, Round round) {
                Round existing = byTournament(tournamentId).putIfAbsent(round.roundNumber(), round);
                if (existing != null) {
                    throw new DuplicateRoundException(
                            "ラウンド" + round.roundNumber() + "は既に生成されています");
                }
            }

            @Override
            public void save(TournamentId tournamentId, Round round) {
                byTournament(tournamentId).put(round.roundNumber(), round);
            }

            private Map<Integer, Round> byTournament(TournamentId tournamentId) {
                return store.computeIfAbsent(tournamentId.value(), k -> new ConcurrentHashMap<>());
            }
        };
    }

    @Bean
    @Primary
    public MatchRepository inMemoryMatchRepository() {
        return new MatchRepository() {
            private final Map<String, Map<String, Match>> store = new ConcurrentHashMap<>();

            @Override
            public Optional<Match> findById(TournamentId tournamentId, MatchId id) {
                return Optional.ofNullable(byTournament(tournamentId).get(id.value()));
            }

            @Override
            public List<Match> findAllByTournamentId(TournamentId tournamentId) {
                return List.copyOf(byTournament(tournamentId).values());
            }

            @Override
            public List<Match> findByRound(TournamentId tournamentId, int roundNumber) {
                return byTournament(tournamentId).values().stream()
                        .filter(m -> m.roundNumber() == roundNumber)
                        .toList();
            }

            @Override
            public void save(TournamentId tournamentId, Match match) {
                Match stored = byTournament(tournamentId).get(match.id().value());
                long storedVersion = stored == null ? 0 : stored.version();
                if (match.version() != storedVersion) {
                    throw new OptimisticLockException("対局が他の操作で更新されています");
                }
                byTournament(tournamentId).put(match.id().value(), new Match(
                        match.id(), match.roundNumber(), match.tableNumber(),
                        match.player1Id(), match.player2Id(), match.result(),
                        match.resultInputBy(), match.player1ReportedResult(),
                        match.player2ReportedResult(), storedVersion + 1, match.groupId()));
            }

            @Override
            public void saveAll(TournamentId tournamentId, List<Match> matches) {
                matches.forEach(m -> save(tournamentId, m));
            }

            private Map<String, Match> byTournament(TournamentId tournamentId) {
                return store.computeIfAbsent(tournamentId.value(), k -> new ConcurrentHashMap<>());
            }
        };
    }

    @Bean
    @Primary
    public GroupRepository inMemoryGroupRepository() {
        return new GroupRepository() {
            private final Map<String, Map<String, Group>> store = new ConcurrentHashMap<>();

            @Override
            public Optional<Group> findById(TournamentId tournamentId, GroupId id) {
                return Optional.ofNullable(byTournament(tournamentId).get(id.value()));
            }

            @Override
            public List<Group> findAllByTournamentId(TournamentId tournamentId) {
                // DynamoDB実装と同様に作成順(ULID=SK昇順)で返す
                return byTournament(tournamentId).values().stream()
                        .sorted(Comparator.comparing(g -> g.id().value()))
                        .toList();
            }

            @Override
            public void save(TournamentId tournamentId, Group group) {
                byTournament(tournamentId).put(group.id().value(), group);
            }

            @Override
            public void delete(TournamentId tournamentId, GroupId id) {
                byTournament(tournamentId).remove(id.value());
            }

            private Map<String, Group> byTournament(TournamentId tournamentId) {
                return store.computeIfAbsent(tournamentId.value(), k -> new ConcurrentHashMap<>());
            }
        };
    }

    @Bean
    @Primary
    public TeamRepository inMemoryTeamRepository() {
        return new TeamRepository() {
            private final Map<String, Map<String, Team>> store = new ConcurrentHashMap<>();

            @Override
            public Optional<Team> findById(TournamentId tournamentId, TeamId id) {
                return Optional.ofNullable(byTournament(tournamentId).get(id.value()));
            }

            @Override
            public List<Team> findAllByTournamentId(TournamentId tournamentId) {
                return List.copyOf(byTournament(tournamentId).values());
            }

            @Override
            public void save(TournamentId tournamentId, Team team) {
                byTournament(tournamentId).put(team.id().value(), team);
            }

            @Override
            public void saveAll(TournamentId tournamentId, List<Team> teams) {
                teams.forEach(t -> save(tournamentId, t));
            }

            @Override
            public void delete(TournamentId tournamentId, TeamId id) {
                byTournament(tournamentId).remove(id.value());
            }

            private Map<String, Team> byTournament(TournamentId tournamentId) {
                return store.computeIfAbsent(tournamentId.value(), k -> new ConcurrentHashMap<>());
            }
        };
    }

    @Bean
    @Primary
    public TeamMatchRepository inMemoryTeamMatchRepository() {
        return new TeamMatchRepository() {
            private final Map<String, Map<String, TeamMatch>> store = new ConcurrentHashMap<>();

            @Override
            public Optional<TeamMatch> findById(TournamentId tournamentId, TeamMatchId id) {
                return Optional.ofNullable(byTournament(tournamentId).get(id.value()));
            }

            @Override
            public List<TeamMatch> findAllByTournamentId(TournamentId tournamentId) {
                return List.copyOf(byTournament(tournamentId).values());
            }

            @Override
            public List<TeamMatch> findByRound(TournamentId tournamentId, int roundNumber) {
                return byTournament(tournamentId).values().stream()
                        .filter(m -> m.roundNumber() == roundNumber)
                        .toList();
            }

            @Override
            public void save(TournamentId tournamentId, TeamMatch match) {
                TeamMatch stored = byTournament(tournamentId).get(match.id().value());
                long storedVersion = stored == null ? 0 : stored.version();
                if (match.version() != storedVersion) {
                    throw new OptimisticLockException("対局が他の操作で更新されています");
                }
                byTournament(tournamentId).put(match.id().value(), new TeamMatch(
                        match.id(), match.roundNumber(), match.tableNumber(),
                        match.team1Id(), match.team2Id(), match.boardResults(),
                        match.resultInputBy(), storedVersion + 1, match.groupId()));
            }

            @Override
            public void saveAll(TournamentId tournamentId, List<TeamMatch> matches) {
                matches.forEach(m -> save(tournamentId, m));
            }

            private Map<String, TeamMatch> byTournament(TournamentId tournamentId) {
                return store.computeIfAbsent(tournamentId.value(), k -> new ConcurrentHashMap<>());
            }
        };
    }
}
