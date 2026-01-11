import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Optional;

public class HurlingTrackerFX extends Application{

    private TrackerService tracker;
    private final ObservableList<TrainingSession> sessionData = FXCollections.observableArrayList();
    private final ObservableList<DrillEntry> drillData = FXCollections.observableArrayList();

    private ListView<TrainingSession> sessionsListView;
    private ListView<DrillEntry> drillsListView;
    private TextArea statsArea;

    @Override
    public void start(Stage stage) {
        CsvStorage storage = new CsvStorage("sessions.csv", "drills.csv", "targets.csv");
        tracker = new TrackerService(storage);
        tracker.load();
        
        refreshSessions();

        TabPane tabs = new TabPane();
        tabs.getTabs().add(makeSessionsTab());
        tabs.getTabs().add(makeDrillsTab());
        tabs.getTabs().add(makeStatsTab());

        stage.setTitle("Hurling Training Tracker");
        stage.setScene(new Scene(tabs, 980, 560));
        stage.show();

        refreshStats();
    }

    private Tab makeSessionsTab() {
        Tab tab = new Tab("Sessions");
        tab.setClosable(false);

        sessionsListView = new ListView<>(sessionData);
        sessionsListView.setPrefWidth(520);
        sessionsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TrainingSession item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.neatOneLine());
            }
        });

        DatePicker datePicker = new DatePicker(LocalDate.now());

        ComboBox<SessionType> typeBox = new ComboBox<>(FXCollections.observableArrayList(SessionType.values()));
        typeBox.getSelectionModel().select(SessionType.GYM);

        TextField minutesField = new TextField();
        minutesField.setPromptText("e.g. 60");

        TextField intensityField = new TextField();
        intensityField.setPromptText("1-5");

        TextField notesField = new TextField();
        notesField.setPromptText("optional");

        Button addBtn = new Button("Add");
        Button deleteBtn = new Button("Delete selected");

        Label status = new Label();

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        form.addRow(0, new Label("Date:"), datePicker);
        form.addRow(1, new Label("Type:"), typeBox);
        form.addRow(2, new Label("Minutes:"), minutesField);
        form.addRow(3, new Label("Intensity:"), intensityField);
        form.addRow(4, new Label("Notes:"), notesField);

        HBox buttons = new HBox(10, addBtn, deleteBtn);

        VBox right = new VBox(12,
            new Label("Add / Manage Sessions"),
            form,
            buttons,
            status 
        );
        right.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setLeft(sessionsListView);
        root.setCenter(right);

        addBtn.setOnAction(e -> {
            try {
                LocalDate date = datePicker.getValue();
                SessionType type = typeBox.getValue();

                int minutes = Integer.parseInt(minutesField.getText().trim());
                int intensity = Integer.parseInt(intensityField.getText().trim());
                if (intensity < 1 || intensity > 5) {
                    status.setText("Intensity must be 1-5");
                    return;
                }

                String notes = notesField.getText() == null ? "" : notesField.getText().trim();

                int id = tracker.nextSessionId();
                TrainingSession s = TrainingSession.create(id, date, type, minutes, intensity, notes);

                tracker.addSession(s);
                tracker.save();
                refreshSessions();
                refreshStats();

                minutesField.clear();
                intensityField.clear();
                notesField.clear();
                status.setText("Added session ID " + s.id());
            } catch (Exception ex) {
                status.setText("Error: " + ex.getMessage());
            }
        });

        deleteBtn.setOnAction(e -> {
            TrainingSession selected = sessionsListView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.setText("Pick a session first.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm delete");
            confirm.setHeaderText("Delete session ID " + selected.id() + "?");
            confirm.setContentText("This also deletes drills linked to this session.");

            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;

            boolean ok = tracker.deleteSession(selected.id());
            if (ok) {
                tracker.save();
                refreshSessions();
                drillData.clear();
                refreshStats();
                status.setText("Deleted session ID " + selected.id());
            } else {
                status.setText("Could not delete.");
            }
        });

        tab.setContent(root);
        return tab;
    }

    private Tab makeDrillsTab() {
        Tab tab = new Tab("Drills");
        tab.setClosable(false);

        ListView<TrainingSession> sessionPick = new ListView<>(sessionData);
        sessionPick.setPrefWidth(520);
        sessionPick.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TrainingSession item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.neatOneLine());
            }
        });

        drillsListView = new ListView<>(drillData);
        drillsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DrillEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.neat());
            }
        });

        ComboBox<DrillType> drillTypeBox = new ComboBox<>(FXCollections.observableArrayList(DrillType.values()));
        drillTypeBox.getSelectionModel().select(DrillType.WALL_BALL);

        TextField repsField = new TextField();
        repsField.setPromptText("E.g. 100");

        TextField successField = new TextField();
        successField.setPromptText("0 if not tracking");

        TextField notesField = new TextField();
        notesField.setPromptText("optional");

        Button addDrillBtn = new Button("Add Drill");
        Label status = new Label("Select a session on the left.");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Drill:"), drillTypeBox);
        form.addRow(1, new Label("Reps:"), repsField);
        form.addRow(2, new Label("Success:"), successField);
        form.addRow(3, new Label("Notes:"), notesField);
        
        VBox right = new VBox(12,
            new Label("Drills for selected session"),
            drillsListView,
            new Separator(),
            new Label("Add Drill"),
            form,
            addDrillBtn,
            status
        );
        right.setPadding(new Insets(10));
        VBox.setVgrow(drillsListView, Priority.ALWAYS);

        SplitPane split = new SplitPane(sessionPick, right);
        split.setDividerPositions(0.55);

        sessionPick.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                drillData.clear();
                status.setText("Select a session on the left.");
            } else {
                refreshDrills(newV.id());
                status.setText("Session ID " + newV.id() + " selected.");
            }
        });

        addDrillBtn.setOnAction(e -> {
            TrainingSession selectedSession = sessionPick.getSelectionModel().getSelectedItem();
            if (selectedSession == null) {
                status.setText("Pick a session first.");
                return;
            }


            try {
                DrillType drillType = drillTypeBox.getValue();
                int reps = Integer.parseInt(repsField.getText().trim());
                int success = Integer.parseInt(successField.getText().trim());
                String notes = notesField.getText() == null ? "" : notesField.getText().trim();

                int drillId = tracker.nextDrillId();
                DrillEntry d = DrillEntry.create(drillId, selectedSession.id(), drillType, reps, success, notes);
                
                tracker.addDrill(d);
                tracker.save();
                
                refreshDrills(selectedSession.id());
                refreshStats();

                repsField.clear();
                successField.clear();
                notesField.clear();
                status.setText("Added drill ID " + d.id() + " to session " + selectedSession.id());
            } catch (Exception ex) {
                status.setText("Error: " + ex.getMessage());
            }
        });

        tab.setContent(split);
        return tab;
            
    }

    private Tab makeStatsTab() {
        Tab tab = new Tab("Stats");
        tab.setClosable(false);

        statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setWrapText(false);

        Button refreshBtn = new Button("Refresh (Last 7 days)");
        refreshBtn.setOnAction(e -> refreshStats());

        VBox root = new VBox(10, refreshBtn, statsArea);
        root.setPadding(new Insets(10));
        VBox.setVgrow(statsArea, Priority.ALWAYS);

        tab.setContent(root);
        return tab;
    }

    private void refreshSessions() {
        sessionData.setAll(tracker.getLastSessions(200));
    }

    private void refreshDrills(int sessionId) {
        drillData.setAll(tracker.getDrillsForSession(sessionId));
    }

    private void refreshStats() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);

        TrackerService.WeeklyStats stats = tracker.getWeeklyStats(from, to);
        Targets targets = tracker.getTargets();

        int gymMinutes = stats.minutesByType()[SessionType.GYM.ordinal()];
        int wallBallReps = stats.repsByDrill()[DrillType.WALL_BALL.ordinal()];

        StringBuilder sb = new StringBuilder();
        sb.append("WEEK REPORT (").append(from).append(" to ").append(to).append(")\n");
        sb.append("-----------------------------------\n");
        sb.append("Sessions: ").append(stats.sessionCount()).append(" / ").append(targets.sessionsPerWeek()).append("\n");
        sb.append("Minutes: ").append(stats.totalMinutes()).append("\n");
        sb.append("Load:    ").append(stats.trainingLoad()).append(" (minutes * intensity)\n\n");

        sb.append("Targets progress:\n");
        sb.append("- Gym minutes: ").append(gymMinutes).append(" / ").append(targets.gymMinutesPerWeek()).append("\n");
        sb.append("- Wall ball reps: ").append(wallBallReps).append(" / ").append(targets.wallBallRepsPerWeek()).append("\n\n");

        sb.append("Minutes by session type:\n");
        SessionType[] st = SessionType.values();
        for (int i = 0; i < st.length; i++) {
            sb.append("- ").append(st[i]).append(": ").append(stats.minutesByType()[i]).append("\n");
        }

        sb.append("\nDrill reps totals:\n");
        DrillType[] dt = DrillType.values();
        boolean any = false;
        for (int i = 0; i < dt.length; i++) {
            int reps = stats.repsByDrill()[i];
            if (reps > 0) {
                any = true;
                sb.append("- ").append(dt[i]).append(": ").append(reps).append("\n");
            }
        }
        if (!any) sb.append("- (none)\n");

        statsArea.setText(sb.toString());
    }
   
    public static void main(String[] args) {
        launch(args);
    }

}

