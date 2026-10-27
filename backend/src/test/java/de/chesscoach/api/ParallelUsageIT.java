package de.chesscoach.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Mehrere Personen (bzw. Browserfenster) nutzen die Anwendung gleichzeitig – auch hinter derselben IP-Adresse –,
 * ohne sich gegenseitig zu beeinflussen.
 */
@Tag("engine")
@SpringBootTest(properties = {"chesscoach.analysis.root-nodes=20000", "chesscoach.analysis.candidate-nodes=8000",
        "chesscoach.analysis.screening-nodes=500", "chesscoach.difficulty.levels.BEGINNER.nodes=200"})
@AutoConfigureMockMvc
class ParallelUsageIT {

    /** Alle simulierten Personen sitzen hinter einem gemeinsamen Router. */
    private static final String SHARED_ADDRESS = "192.168.178.20";

    @Autowired
    MockMvc mvc;

    private MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(r -> {
            r.setRemoteAddr(SHARED_ADDRESS);
            return r;
        })).andReturn().getResponse();
    }

    private MockHttpServletResponse postJson(String path, String body) throws Exception {
        return send(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String newGame() throws Exception {
        MockHttpServletResponse r = postJson("/api/games", "{\"playerColor\":\"WHITE\",\"difficulty\":\"BEGINNER\",\"analysisProfile\":\"FAST\"}");
        assertThat(r.getStatus()).isEqualTo(201);
        return JsonPath.read(r.getContentAsString(), "$.gameId");
    }

    private String move(String uci, int version) {
        return "{\"uciMove\":\"" + uci + "\",\"expectedPositionVersion\":" + version + "}";
    }

    private int version(String gameId) throws Exception {
        return JsonPath.read(send(get("/api/games/" + gameId)).getContentAsString(), "$.positionVersion");
    }

    private String awaitAnalysis(String gameId, int ply) throws Exception {
        for (int i = 0; i < 600; i++) {
            MockHttpServletResponse r = send(get("/api/games/" + gameId + "/analysis/" + ply));
            String status = JsonPath.read(r.getContentAsString(), "$.status");
            if (!status.equals("PENDING") && !status.equals("RUNNING")) {
                return status;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Analyse wurde nicht fertig");
    }

    /** Startet alle Aufgaben gleichzeitig und liefert ihre Ergebnisse in Auftragsreihenfolge. */
    private static <T> List<T> concurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(3, TimeUnit.MINUTES));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void severalPlayersPlayTheirOwnGamesAtTheSameTime() throws Exception {
        String[] openings = {"e2e4", "d2d4", "c2c4", "g1f3", "b2b3", "f2f4"};
        List<Callable<String>> players = new ArrayList<>();
        for (String opening : openings) {
            players.add(() -> {
                String id = newGame();
                MockHttpServletResponse moved = postJson("/api/games/" + id + "/moves", move(opening, version(id)));
                assertThat(moved.getStatus()).isEqualTo(200);
                MockHttpServletResponse answered = postJson("/api/games/" + id + "/engine-move",
                        "{\"expectedPositionVersion\":" + version(id) + "}");
                assertThat(answered.getStatus()).isEqualTo(200);
                assertThat(awaitAnalysis(id, 1)).isEqualTo("COMPLETED");
                return id;
            });
        }

        List<String> ids = concurrently(players);

        assertThat(ids).doesNotHaveDuplicates();
        for (int i = 0; i < ids.size(); i++) {
            String game = send(get("/api/games/" + ids.get(i))).getContentAsString();
            assertThat((Integer) JsonPath.read(game, "$.moves.length()")).isEqualTo(2);
            assertThat((String) JsonPath.read(game, "$.moves[0].uci")).isEqualTo(openings[i]);
            assertThat((String) JsonPath.read(game, "$.sideToMove")).isEqualTo("WHITE");
        }
    }

    @Test
    void undoInOneGameDoesNotCancelAnalysisOfAnotherGame() throws Exception {
        for (int round = 0; round < 3; round++) {
            String a = newGame();
            String b = newGame();
            assertThat(postJson("/api/games/" + b + "/moves", move("d2d4", version(b))).getStatus()).isEqualTo(200);
            assertThat(postJson("/api/games/" + a + "/moves", move("e2e4", version(a))).getStatus()).isEqualTo(200);

            // Person A nimmt ihren Zug sofort zurück, während die Analyse von Person B läuft oder wartet.
            assertThat(postJson("/api/games/" + a + "/undo", "{}").getStatus()).isEqualTo(200);

            assertThat(awaitAnalysis(b, 1)).isEqualTo("COMPLETED");
            assertThat(send(get("/api/games/" + a + "/analysis/1")).getStatus()).isEqualTo(404);
        }
    }

    @Test
    void twoWindowsOnTheSameGameCannotApplyConflictingMoves() throws Exception {
        String id = newGame();
        int v = version(id);

        List<Integer> statuses = concurrently(List.of(
                () -> postJson("/api/games/" + id + "/moves", move("e2e4", v)).getStatus(),
                () -> postJson("/api/games/" + id + "/moves", move("d2d4", v)).getStatus()));

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        String game = send(get("/api/games/" + id)).getContentAsString();
        assertThat((Integer) JsonPath.read(game, "$.moves.length()")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(game, "$.positionVersion")).isEqualTo(v + 1);
    }

    @Test
    void playersBehindTheSameAddressDoNotShareTheirRateLimit() throws Exception {
        // 5 Partien × 100 Anfragen = 500 Anfragen von einer Adresse: unter dem Adress-Limit (600), aber weit über
        // dem früheren gemeinsamen Limit (120). Keine Partie überschreitet ihr eigenes Limit (120).
        List<Callable<Integer>> players = new ArrayList<>();
        for (int p = 0; p < 5; p++) {
            players.add(() -> {
                String id = newGame();
                int limited = 0;
                for (int i = 0; i < 99; i++) {
                    if (postJson("/api/games/" + id + "/undo", "{}").getStatus() == 429) {
                        limited++;
                    }
                }
                return limited;
            });
        }

        assertThat(concurrently(players)).containsOnly(0);
    }
}
