package de.chesscoach.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Startet, überwacht und beendet einen UCI-Engine-Prozess.
 *
 * <p>Das Kommando ist fest konfiguriert; Benutzereingaben werden nie als Argument übergeben.
 * Die Ausgabe wird von einem eigenen Thread gelesen, Zeilen werden in der Länge begrenzt und in eine
 * begrenzte Warteschlange gelegt, damit eine fehlerhafte Engine den Server nicht überlasten kann.</p>
 */
public class UciProcessManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(UciProcessManager.class);
    private static final String EOF = "\u0000EOF";
    private static final int QUEUE_CAPACITY = 20_000;

    private final String name;
    private final List<String> command;
    private final Path workingDirectory;
    private final int maxLineLength;

    private Process process;
    private BufferedWriter writer;
    private BlockingQueue<String> lines;
    private Thread readerThread;
    private int restarts;

    public UciProcessManager(String name, List<String> command, Path workingDirectory, int maxLineLength) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Engine-Kommando fehlt");
        }
        this.name = name;
        this.command = List.copyOf(command);
        this.workingDirectory = workingDirectory;
        this.maxLineLength = maxLineLength;
    }

    public synchronized void start() {
        if (isAlive()) {
            return;
        }
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new EngineUnavailableException("Der Engine-Prozess konnte nicht gestartet werden.", e);
        }
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.US_ASCII));
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        lines = queue;
        Process started = process;
        readerThread = Thread.ofPlatform().daemon().name("uci-reader-" + name).start(() -> readLoop(started, queue));
        log.info("Engine-Prozess '{}' gestartet (pid {})", name, process.pid());
    }

    private void readLoop(Process started, BlockingQueue<String> queue) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > maxLineLength) {
                    line = line.substring(0, maxLineLength);
                }
                if (!queue.offer(line)) {
                    queue.poll();
                    queue.offer(line);
                }
            }
        } catch (IOException e) {
            log.debug("Lesen der Engine-Ausgabe beendet: {}", e.getMessage());
        } finally {
            queue.offer(EOF);
        }
    }

    public synchronized boolean isAlive() {
        return process != null && process.isAlive();
    }

    /** Sendet ein einzelnes UCI-Kommando. Zeilenumbrüche im Kommando sind nicht erlaubt. */
    public synchronized void send(String uciCommand) {
        if (uciCommand.indexOf('\n') >= 0 || uciCommand.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("UCI-Kommando darf keinen Zeilenumbruch enthalten");
        }
        if (!isAlive()) {
            throw new EngineUnavailableException("Der Engine-Prozess läuft nicht.");
        }
        try {
            writer.write(uciCommand);
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            throw new EngineUnavailableException("Kommunikation mit der Engine fehlgeschlagen.", e);
        }
    }

    /**
     * Liefert die nächste Ausgabezeile oder {@code null}, wenn innerhalb des Timeouts keine Zeile kam.
     *
     * @throws EngineUnavailableException wenn der Prozess beendet wurde
     */
    public String readLine(Duration timeout) {
        BlockingQueue<String> queue;
        synchronized (this) {
            queue = lines;
        }
        if (queue == null) {
            throw new EngineUnavailableException("Der Engine-Prozess wurde nicht gestartet.");
        }
        try {
            String line = queue.poll(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
            if (EOF.equals(line)) {
                queue.offer(EOF);
                throw new EngineUnavailableException("Der Engine-Prozess wurde unerwartet beendet.");
            }
            return line;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EngineException("Warten auf die Engine wurde unterbrochen.", e);
        }
    }

    /** Verwirft alle bisher gelesenen, noch nicht verarbeiteten Zeilen. */
    public void discardPendingOutput() {
        BlockingQueue<String> queue;
        synchronized (this) {
            queue = lines;
        }
        if (queue != null) {
            boolean eof = queue.contains(EOF);
            queue.clear();
            if (eof) {
                queue.offer(EOF);
            }
        }
    }

    public synchronized void destroy() {
        if (process == null) {
            return;
        }
        try {
            if (process.isAlive()) {
                try {
                    writer.write("quit\n");
                    writer.flush();
                } catch (IOException ignored) {
                    // Prozess reagiert nicht mehr; wird unten zwangsweise beendet.
                }
                if (!process.waitFor(1, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        } finally {
            process = null;
            writer = null;
        }
    }

    public synchronized void restart() {
        restarts++;
        log.warn("Engine-Prozess '{}' wird neu gestartet (Neustart Nr. {})", name, restarts);
        destroy();
        start();
    }

    public synchronized int restartCount() {
        return restarts;
    }

    public synchronized long pid() {
        return process == null ? -1 : process.pid();
    }

    @Override
    public void close() {
        destroy();
    }
}
