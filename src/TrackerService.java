import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrackerService {
    private final CsvStorage storage;
    
    private final List<TrainingSession> sessions = new ArrayList<>();
    private final List<DrillEntry> drills = new ArrayList<>();

    private int nextSessionId = 1;
    private int nextDrillId = 1;

    public TrackerService(CsvStorage storage) {
        this.storage = storage;
    }

    public void load() {
        sessions.clear();
        drills.clear();
        sessions.addAll(storage.loadSessions());
        drills.addAll(storage.loadDrills());
        sortSessionsNewestFirst();

        int maxSession = 0;
        for (TrainingSession s : sessions) {
            if (s.id() > maxSession) maxSession = s.id();
        }
        nextSessionId = maxSession + 1;

        int maxDrill = 0;
        for (DrillEntry d : drills) {
            if (d.id() > maxDrill) maxDrill = d.id();
        }
        nextDrillId = maxDrill + 1;
    }

    public void save() {
        storage.saveSessions(sessions);
        storage.saveDrills(drills);
    }

    public int nextSessionId() {
        return nextSessionId++;
    }

    public int nextDrillId() {
        return nextDrillId++;
    }

    public void addSession(TrainingSession session) {
        sessions.add(session);
        sortSessionsNewestFirst();
    }

    public void addDrill(DrillEntry drill) {
        drills.add(drill);
    }

    public List<TrainingSession> getLastSessions(int n) {
        List<TrainingSession> out = new ArrayList<>();
        for (int i = 0; i < sessions.size() && i < n; i++) {
            out.add(sessions.get(i));
        }
        return out;
    }

    public List<DrillEntry> getDrillsForSession(int sessionId) {
        List<DrillEntry> out = new ArrayList<>();
        for (DrillEntry d : drills) {
            if (d.sessionId() == sessionId) {
                out.add(d);
            }
        }
        return out;
    }

    public WeeklyStats getWeeklyStats(LocalDate from, LocalDate to) {
        List<TrainingSession> weekSessions = new ArrayList<>();
        for (TrainingSession s : sessions) {
            boolean inRange = !s.date().isBefore(from) && !s.date().isAfter(to);
            if (inRange) {
                weekSessions.add(s);
            }
        }   

        int totalMinutes = 0;
        int load = 0;
        int[] minutesByType = new int[SessionType.values().length];

        for (TrainingSession s : weekSessions) {
            totalMinutes += s.minutes();
            load += s.minutes() * s.intensity();
            minutesByType[s.type().ordinal()] += s.minutes();
        }

        
        
        for  (TrainingSession s : weekSessions) {
            totalMinutes += s.minutes();
            load += s.minutes() * s.intensity();

            int typeIndex = s.type().ordinal();
            minutesByType[typeIndex] += s.minutes();
        }

        Set<Integer> weekSessionIds = new HashSet<>();
        for (TrainingSession s : weekSessions) {
            weekSessionIds.add(s.id());
        }

        int[] repsByDrill = new int[DrillType.values().length];
        int[] successByDrill = new int[DrillType.values().length];

        for (DrillEntry d : drills) {
            if (!weekSessionIds.contains(d.sessionId())) continue;

            int drillIndex = d.drillType().ordinal();
            repsByDrill[drillIndex] += d.reps();
            successByDrill[drillIndex] += d.success();
        }

        return new WeeklyStats(
            weekSessions.size(),
            totalMinutes,
            load,
            minutesByType,
            repsByDrill,
            successByDrill
        );
    }

    private void sortSessionsNewestFirst() {
        sessions.sort(Comparator.comparing(TrainingSession::date).reversed());

    }

    public record WeeklyStats(
        int sessionCount,
        int totalMinutes,
        int trainingLoad,
        int[] minutesByType,
        int[] repsByDrill,
        int[] successByDrill
    ) {}
}
