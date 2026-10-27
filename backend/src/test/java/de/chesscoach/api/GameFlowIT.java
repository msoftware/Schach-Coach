package de.chesscoach.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Durchgängige API-Abläufe mit der echten Engine (Requirements 13.2). */
@Tag("engine")
@SpringBootTest(properties = {"chesscoach.analysis.root-nodes=40000", "chesscoach.analysis.candidate-nodes=20000",
        "chesscoach.analysis.screening-nodes=1000", "chesscoach.difficulty.levels.MEDIUM.nodes=5000"})
@AutoConfigureMockMvc
class GameFlowIT {

    @Autowired
    MockMvc mvc;

    private String json(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    private String newGame() throws Exception {
        MvcResult r = mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerColor\":\"WHITE\",\"difficulty\":\"MEDIUM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.sideToMove").value("WHITE"))
                .andExpect(jsonPath("$.fen").value("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"))
                .andExpect(jsonPath("$.engineStatus").value("READY"))
                .andExpect(jsonPath("$.legalMoves.length()").value(20))
                .andReturn();
        String gameId = JsonPath.read(json(r), "$.gameId");
        assertThat(gameId).matches("[0-9a-f]{32}");
        return gameId;
    }

    private int version(String gameId) throws Exception {
        return JsonPath.read(json(mvc.perform(get("/api/games/" + gameId)).andReturn()), "$.positionVersion");
    }

    private String move(String gameId, String uci, int version) {
        return "{\"uciMove\":\"" + uci + "\",\"expectedPositionVersion\":" + version + "}";
    }

    private String awaitAnalysis(String gameId, int ply) throws Exception {
        for (int i = 0; i < 300; i++) {
            String body = json(mvc.perform(get("/api/games/" + gameId + "/analysis/" + ply)).andReturn());
            String status = JsonPath.read(body, "$.status");
            if (!status.equals("PENDING") && !status.equals("RUNNING")) {
                return body;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Analyse wurde nicht fertig");
    }

    @Test
    void fullFlowWithCoachingEngineMoveUndoAndResign() throws Exception {
        String id = newGame();
        int v = version(id);
        mvc.perform(post("/api/games/" + id + "/moves").contentType(MediaType.APPLICATION_JSON).content(move(id, "e2e4", v)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.san").value("e4"))
                .andExpect(jsonPath("$.positionVersion").value(v + 1))
                .andExpect(jsonPath("$.analysisStatus").value("PENDING"))
                .andExpect(jsonPath("$.replayed").value(false));

        // Wiederholte Anfrage erzeugt keinen doppelten Zug.
        mvc.perform(post("/api/games/" + id + "/moves").contentType(MediaType.APPLICATION_JSON).content(move(id, "e2e4", v)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.game.moves.length()").value(1));

        String analysis = awaitAnalysis(id, 1);
        assertThat((String) JsonPath.read(analysis, "$.status")).isEqualTo("COMPLETED");
        assertThat((String) JsonPath.read(analysis, "$.fenBefore")).isEqualTo("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertThat((Integer) JsonPath.read(analysis, "$.positionVersion")).isEqualTo(v + 1);
        assertThat((String) JsonPath.read(analysis, "$.scoreAfter.type")).isIn("CP", "MATE");
        assertThat((Integer) JsonPath.read(analysis, "$.candidates.length()")).isGreaterThanOrEqualTo(3);
        assertThat((Integer) JsonPath.read(analysis, "$.sections.length()")).isEqualTo(7);
        assertThat((String) JsonPath.read(analysis, "$.engine.name")).isEqualTo("Calvin");

        MvcResult engineMove = mvc.perform(post("/api/games/" + id + "/engine-move").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedPositionVersion\":" + (v + 1) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.game.sideToMove").value("WHITE"))
                .andReturn();
        assertThat((Integer) JsonPath.read(json(engineMove), "$.ply")).isEqualTo(2);

        // Veraltete Version wird mit Konflikt und aktuellem Zustand abgelehnt.
        mvc.perform(post("/api/games/" + id + "/moves").contentType(MediaType.APPLICATION_JSON).content(move(id, "d2d4", v)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("POSITION_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.currentState.positionVersion").value(v + 2));

        // Illegaler Zug: 422, Stellung unverändert.
        mvc.perform(post("/api/games/" + id + "/moves").contentType(MediaType.APPLICATION_JSON).content(move(id, "e1e3", v + 2)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ILLEGAL_MOVE"))
                .andExpect(jsonPath("$.title").value("Ungültiger Zug"))
                .andExpect(jsonPath("$.positionVersion").value(v + 2));

        // Undo nimmt eigenen Zug und Computerantwort zurück und verwirft die Analyse.
        mvc.perform(post("/api/games/" + id + "/undo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedPositionVersion\":" + (v + 2) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moves.length()").value(0))
                .andExpect(jsonPath("$.canRedo").value(true));
        mvc.perform(get("/api/games/" + id + "/analysis/1")).andExpect(status().isNotFound());

        // Redo stellt beide Züge wieder her und fordert eine neue Analyse an.
        mvc.perform(post("/api/games/" + id + "/redo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moves.length()").value(2));
        assertThat((String) JsonPath.read(awaitAnalysis(id, 1), "$.status")).isEqualTo("COMPLETED");

        // Einstellungen ändern markiert Analysen als veraltet.
        mvc.perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":\"" + id + "\",\"analysisProfile\":\"FAST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outdatedAnalyses.length()").value(1));
        mvc.perform(get("/api/games/" + id + "/analysis/1")).andExpect(jsonPath("$.outdated").value(true));

        mvc.perform(post("/api/games/" + id + "/resign").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESIGNED"))
                .andExpect(jsonPath("$.result").value("0-1"));
        mvc.perform(post("/api/games/" + id + "/moves").contentType(MediaType.APPLICATION_JSON).content(move(id, "d2d4", version(id))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_FINISHED"));
    }

    @Test
    void restoreGameAndDetectCheckmate() throws Exception {
        MvcResult r = mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moves\":[\"f2f3\",\"e7e5\",\"g2g4\"],\"playerColor\":\"BLACK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moves.length()").value(3))
                .andExpect(jsonPath("$.sideToMove").value("BLACK"))
                .andReturn();
        String id = JsonPath.read(json(r), "$.gameId");
        int v = JsonPath.read(json(r), "$.positionVersion");
        mvc.perform(post("/api/games/" + id + "/moves").contentType(MediaType.APPLICATION_JSON).content(move(id, "d8h4", v)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKMATE"))
                .andExpect(jsonPath("$.game.winner").value("BLACK"))
                .andExpect(jsonPath("$.game.result").value("0-1"))
                .andExpect(jsonPath("$.game.checkSquare").value("e1"));
        mvc.perform(post("/api/games/" + id + "/engine-move").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_FINISHED"));
        assertThat((String) JsonPath.read(awaitAnalysis(id, 4), "$.classification")).isIn("BEST", "FORCED");
    }

    @Test
    void invalidRestoreIsRejected() throws Exception {
        mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content("{\"moves\":[\"e2e4\",\"e2e4\"]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_RESTORE"));
        mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content("{\"initialFen\":\"8/8/8/8/8/8/8/8 w - - 0 1\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_FEN"));
        mvc.perform(post("/api/games/0000/moves").contentType(MediaType.APPLICATION_JSON).content("{\"uciMove\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/games/" + "a".repeat(32))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void healthEngineStatusRulesAndOpenApi() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.engineAvailable").value(true));
        mvc.perform(get("/api/engine/status")).andExpect(jsonPath("$.engines[0].name").value("Calvin"))
                .andExpect(jsonPath("$.engines[0].version").value("6.1.1"));
        mvc.perform(get("/api/coaching/rules")).andExpect(jsonPath("$.length()").value(28));
        mvc.perform(get("/api/settings/options")).andExpect(jsonPath("$.difficulties.length()").value(5));
        MvcResult api = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        String spec = json(api);
        for (String path : new String[]{"/api/health", "/api/engine/status", "/api/games", "/api/games/{gameId}",
                "/api/games/{gameId}/moves", "/api/games/{gameId}/engine-move", "/api/games/{gameId}/undo",
                "/api/games/{gameId}/restart", "/api/games/{gameId}/analysis/{ply}", "/api/games/{gameId}/analysis/{ply}/variations",
                "/api/settings"}) {
            assertThat(spec).contains("\"" + path + "\"");
        }
        String docs = System.getProperty("chesscoach.docs.dir");
        if (docs != null && Files.isDirectory(Path.of(docs))) {
            Files.writeString(Path.of(docs, "openapi.json"), spec);
        }
    }
}
