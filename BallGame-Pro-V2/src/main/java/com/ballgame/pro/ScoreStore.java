package com.ballgame.pro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/** Persists records in the player's home directory. No account / database needed. */
final class ScoreStore {
    record Entry(int score, int seconds, int maxCombo, int level, String date) {}

    private final Properties properties = new Properties();
    private final Path file = Path.of(System.getProperty("user.home", "."),
            ".ballgame-pro-v2", "records.properties");

    ScoreStore() {
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            } catch (IOException ignored) { /* Game remains playable when disk is unavailable. */ }
        }
    }

    private int integer(String key, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback))); }
        catch (NumberFormatException ex) { return fallback; }
    }

    int gamesPlayed() { return integer("total.games", 0); }
    int allTimeBest() { return integer("total.best", 0); }

    List<Entry> top(String mode, String difficulty) {
        List<Entry> result = new ArrayList<>();
        String prefix = "top." + mode + "." + difficulty + ".";
        for (int i = 0; i < 5; i++) {
            String data = properties.getProperty(prefix + i);
            if (data == null) continue;
            String[] parts = data.split("\\|", -1);
            if (parts.length != 5) continue;
            try {
                result.add(new Entry(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4]));
            } catch (NumberFormatException ignored) { /* Discard damaged row. */ }
        }
        result.sort(Comparator.comparingInt(Entry::score).reversed());
        return result;
    }

    boolean submit(String mode, String difficulty, int score, int seconds, int combo, int level) {
        List<Entry> previous = top(mode, difficulty);
        int previousBest = previous.isEmpty() ? 0 : previous.get(0).score();
        List<Entry> records = new ArrayList<>(previous);
        records.add(new Entry(score, seconds, combo, level,
                DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDateTime.now())));
        records.sort(Comparator.comparingInt(Entry::score).reversed());
        String prefix = "top." + mode + "." + difficulty + ".";
        for (int i = 0; i < 5; i++) {
            if (i < records.size()) {
                Entry e = records.get(i);
                properties.setProperty(prefix + i,
                        e.score() + "|" + e.seconds() + "|" + e.maxCombo() + "|" + e.level() + "|" + e.date());
            } else properties.remove(prefix + i);
        }
        properties.setProperty("total.games", String.valueOf(gamesPlayed() + 1));
        properties.setProperty("total.best", String.valueOf(Math.max(allTimeBest(), score)));
        save();
        return score > previousBest;
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = Files.createTempFile(file.getParent(), "records-", ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                properties.store(out, "BallGame Pro V2 - local high scores");
            }
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (IOException ex) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ignored) { /* Never interrupt a session due to storage permissions. */ }
    }
}
