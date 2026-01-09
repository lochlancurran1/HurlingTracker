import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CsvStorage {
    private final Path sessionsPath;
    private final Path drillsPath;

    public CsvStorage(String sessionsFile, String drillsFile) {
        this.sessionsPath = Paths.get(sessionsFile);
        this.drillsPath = Paths.get(drillsFile);
    }

    public List<TrainingSession> loadSessions() {
        if (!Files.exists(sessionsPath)) return new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(sessionsPath);
            List<TrainingSession> out = new ArrayList<>();
            for (String line : lines) {
                if (!line.isBlank()) out.add(TrainingSession.fromCsv(line));
            }
            return out;
        } catch (IOException e) {
            throw new RuntimeException("Failed reading sessions: " + e.getMessage(), e);
            }
        }
        public void saveSessions(List<TrainingSession> sessions) {
            List<String> lines = new ArrayList<>();
            for (TrainingSession s : sessions) {
                lines.add(s.toCsv());
            }
            writeAll(sessionsPath, lines);
        }

        public List<DrillEntry> loadDrills() {
            if (!Files.exists(drillsPath)) return new ArrayList<>();
            try {
                List<String> lines = Files.readAllLines(drillsPath);
                List<DrillEntry> out = new ArrayList<>();
                for (String line : lines) {
                    if (!line.isBlank()) out.add(DrillEntry.fromCsv(line));
                }
                return out;
            } catch (IOException e) {
                throw new RuntimeException("Failed reading drills: " + e.getMessage(), e);
            }
        }

        public void saveDrills(List<DrillEntry> drills) {
            List<String> lines = new ArrayList<>();
            for (DrillEntry d : drills) {
                lines.add(d.toCsv());
            }
            writeAll(drillsPath, lines);
        }

        private void writeAll(Path path, List<String> lines) {
            try (BufferedWriter bw = Files.newBufferedWriter(path)) {
                for (String line : lines) {
                    bw.write(line);
                    bw.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed writing to " + path + ": " + e.getMessage(), e);
            }
        }
    }


