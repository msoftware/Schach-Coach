package de.chesscoach.infrastructure.logging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Request-ID in Antwort und Log, Zugriffsprotokoll in der rollierenden Datei – ohne Partiedaten. */
@SpringBootTest(properties = {"chesscoach.engine.type=UCI", "chesscoach.engine.uci.command=does-not-exist-engine"})
@AutoConfigureMockMvc
class LoggingIT {

    @Autowired
    MockMvc mvc;

    @Test
    void errorResponseNamesTheRequestIdThatAppearsInTheAccessLog() throws Exception {
        MvcResult r = mvc.perform(get("/api/games"))
                .andExpect(header().exists(AccessLogFilter.REQUEST_ID_HEADER))
                .andReturn();
        String requestId = r.getResponse().getHeader(AccessLogFilter.REQUEST_ID_HEADER);
        mvc.perform(get("/api/games")).andExpect(jsonPath("$.requestId").isString());
        assertThat(r.getResponse().getContentAsString()).contains("\"requestId\":\"" + requestId + "\"");

        Path accessLog = Path.of(System.getProperty("logging.file.path", "logs"), "chess-coach-access.log");
        String content = waitForContent(accessLog, requestId);
        assertThat(content).contains("GET /api/games 405").contains("id=" + requestId);
    }

    /** Gefunden beim Einrichten hinter Apache: Die CORS-Ablehnung erschien nur als nackte 403 ohne Grund. */
    @Test
    void corsRejectionIsExplainedInTheErrorLog() throws Exception {
        mvc.perform(post("/api/games").header("Origin", "https://chat.example.org").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        Path errorLog = Path.of(System.getProperty("logging.file.path", "logs"), "chess-coach-error.log");
        assertThat(waitForContent(errorLog, "https://chat.example.org"))
                .contains("Anfrage vom Ursprung https://chat.example.org abgelehnt (CORS)")
                .contains("server.forward-headers-strategy=native");
    }

    @Test
    void movesAndPositionsAreNotWrittenToTheAccessLog() throws Exception {
        mvc.perform(post("/api/games/" + "c".repeat(32) + "/moves").contentType(MediaType.APPLICATION_JSON)
                .content("{\"uciMove\":\"e2e4\",\"expectedPositionVersion\":0}"));
        Path accessLog = Path.of(System.getProperty("logging.file.path", "logs"), "chess-coach-access.log");
        String content = waitForContent(accessLog, "/moves");
        assertThat(content).contains("POST /api/games/" + "c".repeat(32) + "/moves 404").doesNotContain("e2e4");
    }

    private static String waitForContent(Path file, String expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            if (Files.exists(file)) {
                String content = Files.readString(file);
                if (content.contains(expected)) {
                    return content;
                }
            }
            Thread.sleep(100);
        }
        return Files.exists(file) ? Files.readString(file) : "";
    }
}
