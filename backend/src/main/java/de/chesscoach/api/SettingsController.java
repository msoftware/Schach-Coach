package de.chesscoach.api;

import de.chesscoach.application.game.GameService;
import de.chesscoach.application.game.GameSnapshot;
import de.chesscoach.coaching.CoachingProperties;
import de.chesscoach.domain.game.AnalysisProfile;
import de.chesscoach.domain.game.Difficulty;
import de.chesscoach.dto.request.SettingsRequest;
import de.chesscoach.dto.response.StatusResponses;
import de.chesscoach.infrastructure.config.DifficultyProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Einstellungen", description = "Spielstärke und Analyseprofil der laufenden Sitzung")
public class SettingsController {

    private final GameService games;
    private final DifficultyProperties difficulty;
    private final CoachingProperties coaching;

    public SettingsController(GameService games, DifficultyProperties difficulty, CoachingProperties coaching) {
        this.games = games;
        this.difficulty = difficulty;
        this.coaching = coaching;
    }

    @PutMapping
    @Operation(summary = "Einstellungen für die laufende Sitzung aktualisieren",
            description = "Eine Änderung des Analyseprofils markiert vorhandene Analysen als veraltet.")
    public StatusResponses.SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        GameSnapshot s = games.updateSettings(request.gameId(), request.difficulty(), request.analysisProfile());
        return new StatusResponses.SettingsResponse(s.gameId(), s.difficulty().name(), s.analysisProfile().name(),
                s.settingsVersion(), s.analysisOutdated());
    }

    @GetMapping("/options")
    @Operation(summary = "Verfügbare Spielstärken (mit Engine-Parametern) und Analyseprofile")
    public StatusResponses.SettingsOptionsResponse options() {
        var levels = Arrays.stream(Difficulty.values()).map(d -> {
            DifficultyProperties.Level l = difficulty.level(d);
            return new StatusResponses.DifficultyOption(d.name(), d.german(), l.nodes(), l.depth(), l.movetimeMillis(),
                    l.sampleFraction(), l.describe());
        }).toList();
        var profiles = Arrays.stream(AnalysisProfile.values())
                .map(p -> new StatusResponses.ProfileOption(p.name(), p.german(), p.factor())).toList();
        CoachingProperties.Classification c = coaching.classification();
        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("bestMaxLoss", c.bestMaxLoss());
        thresholds.put("excellentMaxLoss", c.excellentMaxLoss());
        thresholds.put("goodMaxLoss", c.goodMaxLoss());
        thresholds.put("inaccuracyMaxLoss", c.inaccuracyMaxLoss());
        thresholds.put("mistakeMaxLoss", c.mistakeMaxLoss());
        thresholds.put("forcedGap", c.forcedGap());
        return new StatusResponses.SettingsOptionsResponse(levels, profiles, thresholds, coaching.rulesVersion());
    }
}
