package de.chesscoach.api;

import de.chesscoach.application.game.AnalysisQueryService;
import de.chesscoach.dto.response.AnalysisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}/analysis/{ply}")
@Tag(name = "Analyse", description = "Coaching-Analysen einzelner Benutzerzüge")
public class AnalysisController {

    private final AnalysisQueryService analyses;
    private final ApiMapper mapper;

    public AnalysisController(AnalysisQueryService analyses, ApiMapper mapper) {
        this.analyses = analyses;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Analyse eines Zuges abrufen",
            description = "Liefert Status und – sobald abgeschlossen – das vollständige Coaching-Ergebnis.")
    public AnalysisResponse get(@PathVariable String gameId, @PathVariable int ply) {
        return mapper.analysis(gameId, analyses.get(gameId, ply));
    }

    @PostMapping
    @Operation(summary = "Analyse anfordern oder aktualisieren",
            description = "Mit force=true wird eine vorhandene Analyse verworfen und neu berechnet (z. B. nach Einstellungsänderungen).")
    public ResponseEntity<AnalysisResponse> request(@PathVariable String gameId, @PathVariable int ply,
                                                    @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.analysis(gameId, analyses.request(gameId, ply, force)));
    }

    @PostMapping("/variations")
    @Operation(summary = "Kandidaten und Varianten vertiefen",
            description = "Startet eine gründlichere Analyse mit größeren Suchgrenzen und zusätzlichen Kandidaten.")
    public ResponseEntity<AnalysisResponse> deepen(@PathVariable String gameId, @PathVariable int ply) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.analysis(gameId, analyses.deepen(gameId, ply)));
    }
}
