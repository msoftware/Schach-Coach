package de.chesscoach.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Skriptbare Test-Engine, die als eigener Prozess über UCI angesprochen wird.
 * Modi: normal, hang (antwortet erst auf stop), deadlock (ignoriert stop), crash (beendet sich bei go),
 * crash-once:<datei> (stürzt nur beim ersten Start ab), illegal (liefert illegalen Zug), garbage (ungültige bestmove-Zeile),
 * silent (kein uciok), noinfo (bestmove ohne Bewertung).
 */
public final class FakeUciEngine {

    private FakeUciEngine() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "normal";
        boolean crashThisRun = false;
        if (mode.startsWith("crash-once:")) {
            Path marker = Path.of(mode.substring("crash-once:".length()));
            crashThisRun = !Files.exists(marker);
            if (crashThisRun) {
                Files.writeString(marker, "x");
            }
            mode = "normal";
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        boolean searching = false;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.equals("uci")) {
                if (mode.equals("silent")) {
                    continue;
                }
                System.out.println("id name FakeEngine");
                System.out.println("id author Test");
                System.out.println("option name Hash type spin default 16 min 1 max 1024");
                System.out.println("uciok");
            } else if (line.equals("isready")) {
                System.out.println("readyok");
            } else if (line.startsWith("go")) {
                if (crashThisRun || mode.equals("crash")) {
                    System.exit(3);
                }
                switch (mode) {
                    case "hang", "deadlock" -> searching = true;
                    case "illegal" -> {
                        System.out.println("info depth 5 score cp 10 nodes 100 time 1 pv a1a8");
                        System.out.println("bestmove a1a8");
                    }
                    case "garbage" -> System.out.println("bestmove ???");
                    case "noinfo" -> System.out.println("bestmove e2e4");
                    default -> {
                        System.out.println("info depth 1 score cp 15 nodes 20 time 1 pv e2e4");
                        System.out.println("info depth 3 seldepth 5 score cp 25 upperbound nodes 400 time 2 pv d2d4");
                        System.out.println("info depth 3 seldepth 5 score cp 21 nodes 500 time 3 pv e2e4 e7e5 g1f3");
                        System.out.println("info currmove e2e4 currmovenumber 1");
                        System.out.println("bestmove e2e4 ponder e7e5");
                    }
                }
            } else if (line.equals("stop")) {
                if (searching && mode.equals("hang")) {
                    System.out.println("info depth 2 score cp 5 nodes 50 time 5 pv e2e4 e7e5");
                    System.out.println("bestmove e2e4");
                    searching = false;
                }
            } else if (line.equals("quit")) {
                return;
            }
            System.out.flush();
        }
    }
}
