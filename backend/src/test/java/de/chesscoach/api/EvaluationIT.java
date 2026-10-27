package de.chesscoach.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bewertungsleiste: Bewertung der aktuellen Stellung immer aus Sicht von Weiß. */
@Tag("engine")
@SpringBootTest(properties = "chesscoach.analysis.evaluation-nodes=20000")
@AutoConfigureMockMvc
class EvaluationIT {

    @Autowired
    MockMvc mvc;

    private String create(String body) throws Exception {
        String r = mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.gameId");
    }

    private String evaluate(String gameId) throws Exception {
        return mvc.perform(post("/api/games/" + gameId + "/evaluation"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static double share(String evaluation) {
        return ((Number) JsonPath.read(evaluation, "$.whiteShare")).doubleValue();
    }

    @Test
    void startingPositionIsRoughlyBalanced() throws Exception {
        String e = evaluate(create("{}"));
        assertThat((Boolean) JsonPath.read(e, "$.finished")).isFalse();
        assertThat((String) JsonPath.read(e, "$.score.perspective")).isEqualTo("WHITE");
        assertThat((String) JsonPath.read(e, "$.score.type")).isEqualTo("CP");
        assertThat((Integer) JsonPath.read(e, "$.score.value")).isBetween(-100, 100);
        assertThat(share(e)).isBetween(0.35, 0.65);
        assertThat((Integer) JsonPath.read(e, "$.depth")).isPositive();
    }

    @Test
    void evaluationIsFromWhitesViewRegardlessOfSideToMove() throws Exception {
        // Weiß hat eine Dame mehr, Schwarz ist am Zug: die Engine bewertet aus Sicht von Schwarz, die Leiste aus Sicht von Weiß.
        String whiteAhead = evaluate(create("{\"initialFen\":\"4k3/8/8/8/8/8/8/3QK3 b - - 0 1\",\"playerColor\":\"BLACK\"}"));
        assertThat((Integer) JsonPath.read(whiteAhead, "$.score.value")).isPositive();
        assertThat(share(whiteAhead)).isGreaterThan(0.9);

        String blackAhead = evaluate(create("{\"initialFen\":\"3qk3/8/8/8/8/8/8/4K3 w - - 0 1\"}"));
        assertThat((Integer) JsonPath.read(blackAhead, "$.score.value")).isNegative();
        assertThat(share(blackAhead)).isLessThan(0.1);
    }

    @Test
    void finishedGameShowsResultWithoutEngine() throws Exception {
        String id = create("{\"moves\":[\"f2f3\",\"e7e5\",\"g2g4\",\"d8h4\"],\"playerColor\":\"BLACK\"}");
        String e = evaluate(id);
        assertThat((Boolean) JsonPath.read(e, "$.finished")).isTrue();
        assertThat((String) JsonPath.read(e, "$.display")).isEqualTo("0-1");
        assertThat(share(e)).isEqualTo(0.0);
        assertThat((Object) JsonPath.read(e, "$.score")).isNull();
    }

    @Test
    void repeatedRequestForSamePositionIsAnsweredFromCache() throws Exception {
        String id = create("{}");
        String first = evaluate(id);
        String second = evaluate(id);
        assertThat((Integer) JsonPath.read(second, "$.score.value")).isEqualTo(JsonPath.read(first, "$.score.value"));
        assertThat((Integer) JsonPath.read(second, "$.depth")).isEqualTo(JsonPath.read(first, "$.depth"));
    }
}
