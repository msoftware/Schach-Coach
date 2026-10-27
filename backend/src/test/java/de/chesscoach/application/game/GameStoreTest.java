package de.chesscoach.application.game;

import de.chesscoach.application.GameException;
import de.chesscoach.chess.ChessRulesService;
import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.domain.position.Color;
import de.chesscoach.infrastructure.config.ApiProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Bei voller Kapazität wird die am längsten unbenutzte Partie verworfen; neue Partien werden nie abgelehnt. */
class GameStoreTest {

    private final ChessRulesService rules = new ChessRulesService();

    private static ApiProperties properties(int maxGames) {
        return new ApiProperties(List.of(), maxGames, Duration.ofHours(6), 120, 600, 600, Duration.ofMinutes(30));
    }

    private GameSession session(GameStore store) {
        return new GameSession(store.newGameId(), rules.newGame(null), Color.WHITE, Difficulty.MEDIUM, AnalysisProfile.NORMAL);
    }

    @Test
    void fullStoreEvictsLeastRecentlyUsedGameInsteadOfRejecting() throws Exception {
        GameStore store = new GameStore(properties(2));
        GameSession first = session(store);
        GameSession second = session(store);
        store.put(first);
        Thread.sleep(5);
        store.put(second);
        Thread.sleep(5);
        store.get(first.gameId());

        GameSession third = session(store);
        store.put(third);

        assertThat(store.size()).isEqualTo(2);
        assertThat(store.get(first.gameId())).isSameAs(first);
        assertThat(store.get(third.gameId())).isSameAs(third);
        assertThatThrownBy(() -> store.get(second.gameId())).isInstanceOf(GameException.class);
    }

    @Test
    void manyNewGamesNeverBlockFurtherGames() {
        GameStore store = new GameStore(properties(5));
        for (int i = 0; i < 50; i++) {
            store.put(session(store));
        }
        GameSession latest = session(store);
        store.put(latest);
        assertThat(store.size()).isEqualTo(5);
        assertThat(store.get(latest.gameId())).isSameAs(latest);
    }

    @Test
    void concurrentGameStartsNeverExceedCapacityAndNeverFail() throws Exception {
        GameStore store = new GameStore(properties(50));
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < 200; i++) {
                    GameSession s = session(store);
                    store.put(s);
                    store.get(s.gameId());
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(1, TimeUnit.MINUTES);
        }
        pool.shutdown();
        assertThat(store.size()).isEqualTo(50);
    }
}
