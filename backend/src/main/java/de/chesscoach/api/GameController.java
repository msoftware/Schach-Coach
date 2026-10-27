package de.chesscoach.api;

import de.chesscoach.application.game.GameService;
import de.chesscoach.dto.request.CreateGameRequest;
import de.chesscoach.dto.request.RestartRequest;
import de.chesscoach.dto.request.VersionRequest;
import de.chesscoach.dto.response.GameResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Partien", description = "Partie starten, abrufen, neu starten, Züge zurücknehmen und aufgeben")
public class GameController {

    private final GameService games;
    private final ApiMapper mapper;

    public GameController(GameService games, ApiMapper mapper) {
        this.games = games;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Neue Partie starten oder lokal gespeicherte Partie wiederherstellen")
    public GameResponse create(@Valid @RequestBody(required = false) CreateGameRequest request) {
        CreateGameRequest r = request == null ? new CreateGameRequest(null, null, null, null, null) : request;
        return mapper.game(games.create(new GameService.NewGame(r.playerColor(), r.difficulty(), r.analysisProfile(),
                r.initialFen(), r.moves())));
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "Aktuelle Partie abrufen")
    public GameResponse get(@PathVariable String gameId) {
        return mapper.game(games.get(gameId));
    }

    @PostMapping("/{gameId}/undo")
    @Operation(summary = "Letzten eigenen Zug (und die Computerantwort) zurücknehmen")
    public GameResponse undo(@PathVariable String gameId, @Valid @RequestBody(required = false) VersionRequest request) {
        return mapper.game(games.undo(gameId, request == null ? null : request.expectedPositionVersion()));
    }

    @PostMapping("/{gameId}/restart")
    @Operation(summary = "Partie neu starten")
    public GameResponse restart(@PathVariable String gameId, @Valid @RequestBody(required = false) RestartRequest request) {
        RestartRequest r = request == null ? new RestartRequest(null, null, null) : request;
        return mapper.game(games.restart(gameId, r.expectedPositionVersion(), r.playerColor(), r.difficulty()));
    }

    @PostMapping("/{gameId}/resign")
    @Operation(summary = "Partie aufgeben")
    public GameResponse resign(@PathVariable String gameId, @Valid @RequestBody(required = false) VersionRequest request) {
        return mapper.game(games.resign(gameId, request == null ? null : request.expectedPositionVersion()));
    }
}
