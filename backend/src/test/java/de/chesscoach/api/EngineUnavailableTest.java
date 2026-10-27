package de.chesscoach.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ein Engine-Ausfall führt zu einem kontrollierten Fehler – es werden keine Züge oder Analysen simuliert. */
@SpringBootTest(properties = {"chesscoach.engine.type=UCI", "chesscoach.engine.uci.command=does-not-exist-engine",
        "chesscoach.api.rate-limit-per-minute=3"})
@AutoConfigureMockMvc
class EngineUnavailableTest {

    @Autowired
    MockMvc mvc;

    @Test
    void gameStartIsRejectedWithUnderstandableMessage() throws Exception {
        mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ENGINE_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Engine")));
        mvc.perform(get("/api/engine/status"))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.engines[0].state").value("FAILED"));
        mvc.perform(get("/api/health")).andExpect(jsonPath("$.status").value("DEGRADED"));
    }

    @Test
    void rateLimitProtectsEngineEndpoints() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/games/" + "b".repeat(32) + "/hint").with(r -> {
                r.setRemoteAddr("10.0.0.9");
                return r;
            }));
        }
        mvc.perform(post("/api/games/" + "b".repeat(32) + "/hint").with(r -> {
                    r.setRemoteAddr("10.0.0.9");
                    return r;
                }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}
