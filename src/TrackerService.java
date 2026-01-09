import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrackerService {
    private final CsvStorage storage;
    private final List<TrainingSession> sessions = new ArrayList<>():
    private final List<DrillEntry> drills = new ArrayList<>():

    public TrackerService(CsvStorage storage) {
        this.storage = storage;
    }

    public void load() {
        sessions.clear();
        drills.clear();
        sessions.addAll(storage.loadSessions());
        drills.addAll(storage.loadDrills());
        sortSessionsNewestFirst();
    }

    public void save() {
        storage.saveSessions(sessions);
        storage.saveDrills(drills);
    }

    public void addSession(TrainingSession session) {
        sessions.add(session);
        sortSessionsNewestFirst();
    }

    public void addDrill(DrillEntry drill) {
        drills.add(drill);
    }

    public List<TrainingSession> getLastSessions(int n) {
        return sessions.stream().limit(n).toList();
    }

    public List<DrillEntry> getDrillsForSession(String sessionId) {
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
        for (TrainingSession s : weekSessions) {
            totalMinutes += s.minutes();
            load += s.minutes() * s.intensity();
        }

        int[] minutesByType = new int[SessionType.values().length];
        
        for  (TrainingSession s : weekSessions) {
            totalMinutes += s.minutes();
            load += s.minutes() * s.intensity();

            int typeIndex = s.type().oridinal();
            minutesByType[typeIndex] += s.minutes();
        }

        Set<Integer> weekSessionIds = new HashSet<>();
        for (TrainingSession s : weekSessions) {
            weekSessionIds.add(s.id());
        }

        int[] repsByDrill = new int[DrillType.values().length];
        int[] successByDrill = new int[DrillType.values().length];

        for (DrillEntry d : drills) {
            if (!weekSessionsIds.contains(d.sessionId())) continue;

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
