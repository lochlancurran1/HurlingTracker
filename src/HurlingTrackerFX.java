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

    @Override
    public void start(Stage stage) {
        CsvStorage storage = new CsvStorage("sessions.csv", "drills.csv", "targets.csv");
        tracker = new TrackerService(storage);
        tracker.load();
        
        refreshSessions();

        ListView<TrainingSession> sessionList = new ListView<>(sessionData);
        sessionList.setPrefWidth(480);
        sessionList.setCellFactory(lv -> new ListCell<>() {
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
            new Label("Sessions"),
            form,
            buttons,
            status 
        );
        right.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setLeft(sessionList);
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

                minutesField.clear();
                intensityField.clear();
                notesField.clear();
                status.setText("Added session ID " + s.id());
            } catch (Exception ex) {
                status.setText("Error: " + ex.getMessage());
            }
        });

        deleteBtn.setOnAction(e -> {
            TrainingSession selected = sessionList.getSelectionModel().getSelectedItem();
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
                status.setText("Deleted session ID " + selected.id());
            } else {
                status.setText("Could not delete.");
            }
        });

        stage.setTitle("Hurling Training Tracker");
        stage.setScene(new Scene(root, 940, 450));
        stage.show();
    }

    private void refreshSessions() {
        sessionData.setAll(tracker.getLastSessions(200));
    }
    public static void main(String[] args) {
        launch(args);
    }



    }

