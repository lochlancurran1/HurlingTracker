import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class HurlingTrackerApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        CsvStorage storage = new CsvStorage("sessions.csv", "drills.csv", "targets.csv");
        TrackerService tracker = new TrackerService(storage);
        tracker.load();

        tracker.save(); // Ensure targets file exists

        System.out.println("Hurling Training Tracker (Java CLI)");

        while (true) {
            System.out.println("\n-- Menu ---");
            System.out.println("1. Add session");
            System.out.println("2. List sessions (last 10)");
            System.out.println("3. Add drill to a session");
            System.out.println("4. View drills for a session");
            System.out.println("5. Weekly stats (Last 7 days)");
            System.out.println("6. Set Targets");
            System.out.println("7. Delete session");
            System.out.println("8. Delete session");
            System.out.println("9. Print weekly report");
            System.out.println("10. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1" -> addSession(scanner, tracker);
                case "2" -> listSessions(tracker);
                case "3" -> addDrill(scanner, tracker);
                case "4" -> viewDrills(scanner, tracker);
                case "5" -> weeklyStats(tracker);
                case "6" -> setTargets(scanner, tracker);
                case "7" -> deleteSession(scanner, tracker);
                case "8" -> editSession(scanner, tracker);
                case "9" -> printWeeklyReport(tracker);
                case "10" -> {
                    tracker.save();
                    System.out.println("Saved. Exiting.");
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void addSession(Scanner scanner, TrackerService tracker) {
        try {
            LocalDate date = readDate(scanner);
            SessionType type = readSessionType(scanner);

            int minutes = readInt(scanner, "Minutes: ");
            int intensity = readInt(scanner, "Intensity 1-5: ");
            if (intensity < 1 || intensity > 5) throw new IllegalArgumentException("Inensity must be 1-5");

            System.out.print("Notes (optional): ");
            String notes = scanner.nextLine().trim();

            int id = tracker.nextSessionId();
            TrainingSession session = TrainingSession.create(id, date, type, minutes, intensity, notes);
            tracker.addSession(session);
            tracker.save();

            System.out.println("Added:\n" + session.neat());
        } catch (Exception e) {
            System.out.println("Error adding session: " + e.getMessage());
        }
        }

        private static void listSessions(TrackerService tracker) {
            List<TrainingSession> sessions = tracker.getLastSessions(10);
            if (sessions.isEmpty()) {
                System.out.println("No sessions logged yet.");
                return;
            }
            System.out.println("\nLast sessions:");
            for (TrainingSession s : sessions) {
                System.out.println(s.neatOneLine());
            }
        }

        private static void addDrill(Scanner scanner, TrackerService tracker) {
            try {
                TrainingSession session = pickSession(scanner, tracker);
                if (session == null) return;

                DrillType drillType = readDrillType(scanner);

                int reps = readInt(scanner, "Reps (e.g. strikes, sprints, gym reps): ");
                int success = readInt(scanner, "Success (0 if not tracking): ");
                
                System.out.print("Notes (optional): ");
                String notes = scanner.nextLine().trim();

                int drillId = tracker.nextDrillId();
                DrillEntry drill = DrillEntry.create(drillId, session.id(), drillType, reps, success, notes);
                
                tracker.addDrill(drill);
                tracker.save();

                System.out.println("Added drill:\n" + drill.neat());
            } catch (Exception e) {
                System.out.println("Couldn't add drill: " + e.getMessage());
            }
         }

         private static void viewDrills(Scanner scanner, TrackerService tracker) {
            TrainingSession session = pickSession(scanner, tracker);
            if (session == null) return;

            List<DrillEntry> drills = tracker.getDrillsForSession(session.id());
            if (drills.isEmpty()) {
                System.out.println("No drills logged for that session.");
                return;
            }

            System.out.println("\nSessions:");
            System.out.println(session.neat());
            System.out.println("\nDrills:");
            for (DrillEntry d : drills) {
                System.out.println(d.neat());
            }
         }

         private static void weeklyStats(TrackerService tracker) {
            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(6);

            TrackerService.WeeklyStats stats = tracker.getWeeklyStats(from, to);

            System.out.println("\nLast 7 days (" + from + " to " + to + ")");
            System.out.println("Total sessions: " + stats.sessionCount());
            System.out.println("Total minutes: " + stats.totalMinutes());
            System.out.println("Load:    " + stats.trainingLoad() + " (minutes * intensity)");

            System.out.println("\nMinutes by session type:");
            SessionType[] sessionTypes = SessionType.values();
            for (int i = 0; i < sessionTypes.length; i++) {
                System.out.println(" - " + sessionTypes[i] + ": " + stats.minutesByType()[i]);
            }

            System.out.println("\nDrill reps totals:");
            DrillType[] drillTypes = DrillType.values();
            for (int i = 0; i < drillTypes.length; i++) {
                int reps = stats.repsByDrill()[i];
                if (reps > 0) System.out.println(" - " + drillTypes[i] + ": " + reps);
            }
        
            System.out.println("\nDrill success totals:");
            for (int i = 0; i < drillTypes.length; i++) {
                int success = stats.successByDrill()[i];
                if (success > 0) System.out.println(" - " + drillTypes[i] + ": " + success);
         }
        }

        public static void setTargets(Scanner scanner, TrackerService tracker) {
            Targets cur = tracker.getTargets();

            System.out.println("\nCurrent targets:");
            System.out.println("1. Sessions per week: " + cur.sessionsPerWeek());
            System.out.println("2. Wall ball reps/week: " + cur.wallBallRepsPerWeek());
            System.out.println("3. Gym minutes/week: " + cur.gymMinutesPerWeek());
            System.out.println("\nEnter new values or leave blank to keep current.");

            Integer sessions = readOptionalInt(scanner, "Sessions per week: ");
            Integer wallBall = readOptionalInt(scanner, "Wall ball reps/week: ");
            Integer gym = readOptionalInt(scanner, "Gym minutes/week: ");

            Targets updated = new Targets(
                sessions == null ? cur.sessionsPerWeek() : sessions,
                wallBall == null ? cur.wallBallRepsPerWeek() : wallBall,
                gym == null ? cur.gymMinutesPerWeek() : gym
            );

            tracker.setTargets(updated);
            tracker.save();
            System.out.println("Targets saved.");
        }

        private static void deleteSession(Scanner scanner, TrackerService tracker) {
            TrainingSession session = pickSession(scanner, tracker);
            if (session == null) return;

            System.out.print("Delete session ID " + session.id() + " (and its drills)? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("y")){
                System.out.println("Cancelled.");
                return;
            }

            boolean ok = tracker.deleteSession(session.id());
            if (ok) {
                tracker.save();
                System.out.println("Deleted.");
            } else {
                System.out.println("Could not delete (not found).");
            }
  }

        private static void editSession(Scanner scanner, TrackerService tracker) {
            TrainingSession session = pickSession(scanner, tracker);
            if (session == null) return;

            System.out.println("Editing session:\n");
            System.out.println(session.neatOneLine());
            System.out.println("Leave blank to keep existing.\n");

            LocalDate newDate = readOptionalDate(scanner, "Date (YYYY-MM-DD): ");
            SessionType newType = readOptionalSessionType(scanner, "New type: ");
            Integer newMinutes = readOptionalInt(scanner, "New minutes: ");
            Integer newIntensity = readOptionalInt(scanner, "New intensity (1-5): ");
            String newNotes = readOptionalString(scanner, "New notes: ");

            TrainingSession updated = new TrainingSession(
                session.id(),
                newDate == null ? session.date() : newDate,
                newType == null ? session.type() : newType,
                newMinutes == null ? session.minutes() : newMinutes,
                newIntensity == null ? session.intensity() : newIntensity,
                newNotes == null ? session.notes() : newNotes
            );

            boolean ok = tracker.updateSession(updated);
            if (ok) {
                tracker.save();
                System.out.println("Updated:\n" + updated.neat());
            } else {
                System.out.println("Could not update (not found).");
            }
        }

        private static void printWeeklyReport(TrackerService tracker) {
            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(6);

            TrackerService.WeeklyStats stats = tracker.getWeeklyStats(from, to);
            Targets targets = tracker.getTargets();

            int gymMinutes = stats.minutesByType()[SessionType.GYM.ordinal()];
            int wallBallReps = stats.repsByDrill()[DrillType.WALL_BALL.ordinal()];

            System.out.println("\n==============================");
        System.out.println("WEEK REPORT (" + from + " to " + to + ")");
        System.out.println("==============================");
        System.out.println("Sessions: " + stats.sessionCount() + " / " + targets.sessionsPerWeek());
        System.out.println("Minutes:  " + stats.totalMinutes());
        System.out.println("Load:     " + stats.trainingLoad());
        System.out.println();
        System.out.println("Targets progress:");
        System.out.println("- Gym minutes: " + gymMinutes + " / " + targets.gymMinutesPerWeek());
        System.out.println("- Wall ball reps: " + wallBallReps + " / " + targets.wallBallRepsPerWeek());
        System.out.println();

        System.out.println("By session type (minutes):");
        SessionType[] sessionTypes = SessionType.values();
        for (int i = 0; i < sessionTypes.length; i++) {
            System.out.println(" - " + sessionTypes[i] + ": " + stats.minutesByType()[i]);
        }
        System.out.println();
        

        System.out.println("Drills (reps):");
        DrillType[] drillTypes = DrillType.values();
        boolean any = false;
        for (int i = 0; i < drillTypes.length; i++) {
            int reps = stats.repsByDrill()[i];
            if (reps > 0) {
                any = true;
                System.out.println("- " + drillTypes[i] + ": " + reps);
            }
        }
        if (!any) System.out.println("- (none)");
        System.out.println("==============================\n");
    }
         

         private static LocalDate readDate(Scanner scanner) {
            System.out.print("Date (YYYY-MM-DD) or blank for today: ");
            String s = scanner.nextLine().trim();
            return s.isEmpty() ? LocalDate.now() : LocalDate.parse(s);
         }

         private static SessionType readSessionType(Scanner scanner) {
            System.out.println("Session types: " + Arrays.toString(SessionType.values()));
            System.out.print("Type: ");
            return SessionType.valueOf(scanner.nextLine().trim().toUpperCase());
         }

         private static DrillType readDrillType(Scanner scanner) {
            System.out.println("Drill types: " + Arrays.toString(DrillType.values()));
            System.out.print("Drill: ");
            return DrillType.valueOf(scanner.nextLine().trim().toUpperCase());
         }

         private static int readInt(Scanner scanner, String prompt) {
            System.out.print(prompt);
            return Integer.parseInt(scanner.nextLine().trim());
         }

         private static Integer readOptionalInt(Scanner scanner, String prompt) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
         }

         private static String readOptionalString(Scanner scanner, String prompt) {
            System.out.print(prompt);
            String s = scanner.nextLine();
            if (s == null) return null;
            s = s.trim();
            return s.isEmpty() ? null : s;
         }

         private static LocalDate readOptionalDate(Scanner scanner, String prompt) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (s.isEmpty()) return null;
            return LocalDate.parse(s);
         }

         private static SessionType readOptionalSessionType(Scanner scanner, String prompt) {
            System.out.println("Session types: " + Arrays.toString(SessionType.values()));
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (s.isEmpty()) return null;
            return SessionType.valueOf(s.toUpperCase());
         }


         private static TrainingSession pickSession(Scanner scanner, TrackerService tracker) {
            List<TrainingSession> sessions = tracker.getLastSessions(10);
            if (sessions.isEmpty()) {
                System.out.println("No sessions yet. Add one first.");
                return null;
            }

            System.out.println("\nPick a session:");
            for (int i = 0; i < sessions.size(); i++) {
                System.out.println((i + 1) + ") " + sessions.get(i).neatOneLine());
            }
            System.out.print("> ");

            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx < 0 || idx >= sessions.size()) {
                System.out.println("Invalid selection.");
                return null;
            }
            return sessions.get(idx);
         }



       
       
        }
    


