package de.chesscoach.engine;

import de.chesscoach.domain.position.MoveOracle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * UCI-Integration für die Calvin-Engine (https://github.com/kelseyde/calvin-chess-engine, MIT-Lizenz).
 *
 * <p>Calvin-Besonderheiten der Version 6.1.1, die durch Tests abgesichert sind:</p>
 * <ul>
 *     <li>Start als JAR mit {@code --add-modules jdk.incubator.vector} (Vector API für das neuronale Netz).</li>
 *     <li>{@code go depth N} sucht bis Tiefe N-1; deshalb wird die angeforderte Tiefe um eins erhöht.</li>
 *     <li>Keine Unterstützung für MultiPV und {@code searchmoves}; Kandidatenzüge werden deshalb über
 *     die Bewertung der Folgestellungen ermittelt (siehe AnalysisService).</li>
 *     <li>In Stellungen ohne legale Züge liefert Calvin einen veralteten {@code bestmove}; solche Stellungen
 *     werden nie an die Engine geschickt, und jeder gelieferte Zug wird auf Legalität geprüft.</li>
 * </ul>
 */
public class CalvinChessEngine extends UciChessEngine {

    public CalvinChessEngine(UciProcessManager process, Settings settings, MoveOracle moveOracle) {
        super(process, settings, moveOracle);
    }

    /** Baut das feste Startkommando für das Calvin-JAR. */
    public static List<String> command(Path javaExecutable, Path jarPath, int maxHeapMb) {
        if (!Files.isRegularFile(jarPath)) {
            throw new EngineUnavailableException("Die Engine-Datei wurde nicht gefunden: " + jarPath.getFileName());
        }
        List<String> command = new ArrayList<>();
        command.add(javaExecutable.toString());
        command.add("-Xmx" + maxHeapMb + "m");
        command.add("-XX:+UseSerialGC");
        command.add("--add-modules");
        command.add("jdk.incubator.vector");
        command.add("-jar");
        command.add(jarPath.toAbsolutePath().toString());
        return command;
    }

    /** Java-Laufzeit, mit der auch das Backend läuft. */
    public static Path currentJava() {
        String javaHome = System.getProperty("java.home");
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return Path.of(javaHome, "bin", windows ? "java.exe" : "java");
    }

    @Override
    protected String goCommand(AnalysisLimits limits) {
        if (limits.depth() == null) {
            return super.goCommand(limits);
        }
        return super.goCommand(new AnalysisLimits(limits.depth() + 1, limits.nodes(), limits.movetimeMillis()));
    }
}
