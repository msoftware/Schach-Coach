package de.chesscoach.api;

import de.chesscoach.application.game.EvaluationService;
import de.chesscoach.application.game.MoveService;
import de.chesscoach.dto.request.MoveRequest;
import de.chesscoach.dto.request.VersionRequest;
import de.chesscoach.dto.response.GameResponse;
import de.chesscoach.dto.response.MoveResponse;
import de.chesscoach.dto.response.ScoreDto;
import de.chesscoach.dto.response.StatusResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}")
@Tag(name = "Züge", description = "Benutzerzüge, Computerzüge, Wiederholen, Tipps und Stellungsbewertung")
public class MoveController {

    private final MoveService moves;
    private final EvaluationService evaluations;
    private final ApiMapper mapper;

    public MoveController(MoveService moves, EvaluationService evaluations, ApiMapper mapper) {
        this.moves = moves;
        this.evaluations = evaluations;
        this.mapper = mapper;
    }

    @PostMapping("/moves")
    @Operation(summary = "Benutzerzug ausführen",
            description = "Prüft den Zug atomar gegen die erwartete Positionsversion. Die Coaching-Analyse startet automatisch "
                    + "und asynchron (analysisStatus = PENDING). Eine wiederholte identische Anfrage führt den Zug nicht erneut aus.")
    public MoveResponse move(@PathVariable String gameId, @Valid @RequestBody MoveRequest request) {
        return mapper.move(moves.playUserMove(gameId, request.uciMove(), request.expectedPositionVersion()));
    }

    @PostMapping("/engine-move")
    @Operation(summary = "Computerzug anfordern", description = "Berechnet den Zug mit der integrierten Engine (synchron).")
    public MoveResponse engineMove(@PathVariable String gameId, @Valid @RequestBody(required = false) VersionRequest request) {
        return mapper.move(moves.requestEngineMove(gameId, request == null ? null : request.expectedPositionVersion()));
    }

    @PostMapping("/redo")
    @Operation(summary = "Zurückgenommenen Zug wiederherstellen")
    public GameResponse redo(@PathVariable String gameId, @Valid @RequestBody(required = false) VersionRequest request) {
        return mapper.game(moves.redo(gameId, request == null ? null : request.expectedPositionVersion()));
    }

    @PostMapping("/hint")
    @Operation(summary = "Tipp für die aktuelle Stellung (bester Zug laut Engine)")
    public StatusResponses.HintResponse hint(@PathVariable String gameId) {
        MoveService.Hint hint = moves.hint(gameId);
        return new StatusResponses.HintResponse(hint.uci(), hint.san(), hint.from(), hint.pieceName(), hint.score(), hint.depth(),
                "ENGINE");
    }

    @PostMapping("/evaluation")
    @Operation(summary = "Bewertung der aktuellen Stellung (Bewertungsleiste)",
            description = "Bewertet die aktuelle Stellung mit festen Suchgrenzen, unabhängig von der Spielstärke, immer aus "
                    + "Sicht von Weiß. whiteShare (0..1) gibt an, wie weit die Leiste zu Weiß ausschlägt.")
    public StatusResponses.EvaluationResponse evaluation(@PathVariable String gameId) {
        EvaluationService.Evaluation e = evaluations.evaluate(gameId);
        return new StatusResponses.EvaluationResponse(e.gameId(), e.positionVersion(), e.fen(), ScoreDto.ofWhite(e.whiteScore()),
                e.whiteShare(), e.display(), e.depth(), e.finished());
    }
}
