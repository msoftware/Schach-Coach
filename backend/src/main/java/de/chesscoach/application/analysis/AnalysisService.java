package de.chesscoach.application.analysis;

import de.chesscoach.domain.analysis.MoveAnalysis;
import de.chesscoach.engine.EngineException;
import de.chesscoach.engine.EngineRegistry;
import de.chesscoach.engine.EngineTimeoutException;
import de.chesscoach.engine.EngineUnavailableException;
import de.chesscoach.infrastructure.config.AnalysisProperties;
import de.chesscoach.infrastructure.logging.AccessLogFilter;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Orchestriert Coaching-Analysen asynchron auf der Analyse-Engine. Es läuft immer höchstens eine Analyse
 * gleichzeitig (eine Engine-Instanz), weitere warten in einer begrenzten Warteschlange. Veraltete Analysen
 * (nach Undo oder Neustart) werden abgebrochen und ihr Ergebnis verworfen.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final EngineRegistry engines;
    private final MoveAnalyzer analyzer;
    private final ThreadPoolExecutor executor;
    /** Schützt {@link #running}, damit ein Abbruch nie die Suche einer anderen Partie stoppt. */
    private final Object runningLock = new Object();
    private AnalysisEntry running;

    public AnalysisService(EngineRegistry engines, MoveAnalyzer analyzer, AnalysisProperties properties) {
        this.engines = engines;
        this.analyzer = analyzer;
        this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.max(1, properties.maxQueuedAnalyses())),
                runnable -> Thread.ofPlatform().daemon().name("coaching-analysis").unstarted(runnable));
    }

    /** Reiht eine Analyse ein. Bei voller Warteschlange wird der Auftrag als fehlgeschlagen markiert. */
    public void submit(AnalysisEntry entry) {
        // Log-Kontext (Request-ID, Partie) der auslösenden Anfrage in den Analyse-Thread übernehmen.
        Map<String, String> context = MDC.getCopyOfContextMap();
        try {
            executor.execute(() -> {
                if (context != null) {
                    MDC.setContextMap(context);
                }
                MDC.put(AccessLogFilter.GAME_ID, AccessLogFilter.shortGameId(entry.gameId()));
                try {
                    run(entry);
                } finally {
                    MDC.clear();
                }
            });
            log.debug("Analyse von Halbzug {} eingereiht ({} wartend)", entry.move().ply(), executor.getQueue().size());
        } catch (RejectedExecutionException e) {
            log.warn("Analyse-Warteschlange voll ({} wartend) – Analyse von Halbzug {} abgelehnt",
                    executor.getQueue().size(), entry.move().ply());
            entry.fail("Es laufen zu viele Analysen gleichzeitig. Bitte fordere die Analyse später erneut an.");
        }
    }

    private void run(AnalysisEntry entry) {
        if (entry.token().isCancelled()) {
            return;
        }
        long started = System.currentTimeMillis();
        synchronized (runningLock) {
            running = entry;
        }
        entry.running();
        try {
            MoveAnalyzer.Request request = new MoveAnalyzer.Request(entry.move().fenBefore(), entry.move().uci(),
                    entry.previousOpponentMove(), entry.previousOwnMove(), entry.profile(), entry.depthLevel());
            MoveAnalysis analysis = analyzer.analyze(engines.analysis(), request, entry.token(), entry::progress);
            if (entry.token().isCancelled()) {
                return;
            }
            entry.complete(analysis);
            log.info("Analyse von Halbzug {} ({}) fertig in {} ms: {}, Tiefe {}, {} Knoten, Profil {}, Stufe {}",
                    entry.move().ply(), entry.move().san(), System.currentTimeMillis() - started, analysis.classification(),
                    analysis.rootDepth(), analysis.engineNodes(), entry.profile(), entry.depthLevel());
        } catch (CancellationException e) {
            log.debug("Analyse für Zug {} abgebrochen", entry.move().ply());
        } catch (EngineTimeoutException e) {
            log.warn("Analyse von Halbzug {} wegen Zeitüberschreitung der Engine abgebrochen", entry.move().ply());
            entry.fail("Die Engine hat nicht rechtzeitig geantwortet. Die Analyse wurde abgebrochen.");
        } catch (EngineUnavailableException e) {
            log.warn("Analyse von Halbzug {} nicht möglich: Engine nicht verfügbar", entry.move().ply());
            entry.fail("Die Schach-Engine ist nicht verfügbar. Eine Analyse ist derzeit nicht möglich.");
        } catch (EngineException e) {
            log.warn("Analyse von Halbzug {} wegen Engine-Fehler verworfen: {}", entry.move().ply(), e.getMessage());
            entry.fail("Die Engine hat eine ungültige Ausgabe geliefert. Die Analyse wurde verworfen.");
        } catch (RuntimeException e) {
            log.error("Analyse fehlgeschlagen", e);
            entry.fail("Bei der Analyse ist ein interner Fehler aufgetreten.");
        } finally {
            synchronized (runningLock) {
                running = null;
            }
        }
    }

    /**
     * Verwirft eine Analyse; läuft sie gerade, wird die Engine-Suche gestoppt. Prüfung und Stopp erfolgen unter
     * derselben Sperre wie der Wechsel der laufenden Analyse, damit nie die Analyse einer anderen Partie (eines
     * anderen Spielers) abgebrochen wird.
     */
    public void cancel(AnalysisEntry entry) {
        entry.cancel();
        synchronized (runningLock) {
            if (running == entry) {
                engines.analysis().stopAnalysis();
            }
        }
    }

    public int queueSize() {
        return executor.getQueue().size();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
