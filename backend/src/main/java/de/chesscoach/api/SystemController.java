package de.chesscoach.api;

import de.chesscoach.application.analysis.AnalysisService;
import de.chesscoach.application.game.GameStore;
import de.chesscoach.coaching.CoachingProperties;
import de.chesscoach.coaching.CoachingRule;
import de.chesscoach.coaching.StrategicRuleEngine;
import de.chesscoach.coaching.TacticalRuleEngine;
import de.chesscoach.dto.response.StatusResponses;
import de.chesscoach.engine.EngineInfo;
import de.chesscoach.engine.EngineRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "System", description = "Gesundheitsstatus, Engine-Status und Regelkatalog")
public class SystemController {

    private final EngineRegistry engines;
    private final GameStore store;
    private final AnalysisService analysisService;
    private final TacticalRuleEngine tactical;
    private final StrategicRuleEngine strategic;
    private final CoachingProperties coaching;

    public SystemController(EngineRegistry engines, GameStore store, AnalysisService analysisService,
                            TacticalRuleEngine tactical, StrategicRuleEngine strategic, CoachingProperties coaching) {
        this.engines = engines;
        this.store = store;
        this.analysisService = analysisService;
        this.tactical = tactical;
        this.strategic = strategic;
        this.coaching = coaching;
    }

    @GetMapping("/health")
    @Operation(summary = "Gesundheitsstatus der Anwendung")
    public StatusResponses.HealthResponse health() {
        boolean available = engines.status().stream().allMatch(EngineInfo::available);
        return new StatusResponses.HealthResponse(available ? "UP" : "DEGRADED", available, store.size(), "1.0.0");
    }

    @GetMapping("/engine/status")
    @Operation(summary = "Verfügbarkeit und Version der Engine")
    public StatusResponses.EngineStatusResponse engineStatus() {
        List<StatusResponses.EngineInstance> instances = engines.status().stream()
                .map(i -> new StatusResponses.EngineInstance(i.role(), i.name(), i.author(), i.version(), i.state().name(),
                        i.message(), i.supportsMultiPv()))
                .toList();
        boolean available = engines.play().getEngineInfo().available();
        return new StatusResponses.EngineStatusResponse(available, instances, analysisService.queueSize());
    }

    @GetMapping("/coaching/rules")
    @Operation(summary = "Katalog der implementierten Coaching-Regeln")
    public List<StatusResponses.RuleDto> rules() {
        List<CoachingRule> all = new ArrayList<>(tactical.rules());
        all.addAll(strategic.rules());
        return all.stream().map(r -> new StatusResponses.RuleDto(r.id(), r.title(), r.category().name(), r.version(), r.purpose(),
                r.inputFeatures(), r.learningPrinciple(), !coaching.disabledRules().contains(r.id()))).toList();
    }
}
