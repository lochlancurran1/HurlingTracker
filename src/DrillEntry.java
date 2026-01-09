public record DrillEntry(
    int id,
    int sessionId,
    DrillType drillType,
    int reps,
    int success,
    String notes
) {
    public static DrillEntry create(int id, int sessionId, DrillType drillType, int reps, int success, String notes) {
        return new DrillEntry(
            id,
            sessionId,
            drillType,
            reps,
            success,
            notes == null ? "" : notes
        );
    }
    public String neat() {
        String n = notes.isBlank() ? "" : " | " + notes;
        String suc = success > 0 ? (" | success " + success) : "";
        return "- " + drillType + " reps " + reps + suc + n + " | ID " + id;
    }

    public String toCsv() {
        return id + "," + sessionId + "," + drillType + "," + reps + "," + success + "," + CsvUtil.clean(notes);
    }

    public static DrillEntry fromCsv(String line) {
        var cols = CsvUtil.parse(line);
        if (cols.size() < 6) throw new IllegalArgumentException("Bad drill row: " + line);
        
        return new DrillEntry(
            Integer.parseInt(cols.get(0)),
            Integer.parseInt(cols.get(1)),
            DrillType.valueOf(cols.get(2)),
            Integer.parseInt(cols.get(3)),
            Integer.parseInt(cols.get(4)),
            cols.get(5)
        );
    }

}
