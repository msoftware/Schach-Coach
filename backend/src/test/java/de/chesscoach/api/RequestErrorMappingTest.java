package de.chesscoach.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fehlerhafte Anfragen (typisch hinter falsch konfigurierten Proxys) werden mit ihrem echten Status gemeldet – nicht als
 * „interner Fehler“, der die Ursache verschleiert.
 */
@SpringBootTest(properties = {"chesscoach.engine.type=UCI", "chesscoach.engine.uci.command=does-not-exist-engine"})
@AutoConfigureMockMvc
class RequestErrorMappingTest {

    @Autowired
    MockMvc mvc;

    @Test
    void wrongMethodIsReportedAs405() throws Exception {
        // z. B. wenn eine http→https-Weiterleitung mit 301/302 aus einem POST ein GET macht
        mvc.perform(get("/api/games"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.detail").value(containsString("GET")));
    }

    @Test
    void wrongContentTypeIsReportedAs415() throws Exception {
        mvc.perform(post("/api/games").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }
}
