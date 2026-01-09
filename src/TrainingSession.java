import java.time.LocalDate;


public record TrainingSession(int id, LocalDate date,
    SessionType type, int minutes, int intensity, String notes) 
    {
        public static TrainingSession create(int id, LocalDate date, SessionType type,
            int minutes, int intensity, String notes) {
                return new TrainingSession(
                    id,
                    date,
                    type,
                    minutes,
                    intensity, notes == null ? "" : notes
                );
            }

        public String neat() {
            String notesText = notes.isBlank() ? "-" : notes;
            return date + "\n" +
                "Type: " + type + "\n" +
                "Minutes: " + minutes + " \n" +
                "Intensity: " + intensity + "\n" +
                "Notes: " + notesText + "\n" +
                "ID: " + id;
            
        }
        public String neatOneLine() {
            String n = notes.isBlank() ? "" : " | " + notes;
            return date + " | " + type + " | " + minutes + " min | int " + intensity + n + " | ID " + id;
        }
        public String toCsv() {
            return String.join(",",
                Integer.toString(id),
                CsvUtil.esc(id),
                CsvUtil.esc(date.toString()),
                CsvUtil.esc(type.name()),
                Integer.toString(minutes),
                Integer.toString(intensity),
                CsvUtil.esc(notes)
            );
        }
        public static TrainingSession fromCsv(String line) {
            var cols = CsvUtil.parse(line);
            if (cols.size() < 6) throw new IllegalArgumentException("Bad session row: " + line);
            
            return new TrainingSession(
                Integer.parseInt(cols.get(0)), LocalDate.parse(cols.get(1)),
                SessionType.valueOf(cols.get(2)),
                Integer.parseInt(cols.get(3)),
                Integer.parseInt(cols.get(4)),
                cols.get(5)
            );
        }
        

    }



