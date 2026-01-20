public record DrillEntry(int id, int sessionId, DrillType drillType,
    int reps, int success, String notes) {

     
    public static DrillEntry create(int id, int sessionId, DrillType drillType, int reps, int success, String notes) {
        String safeNotes;
        if (notes == null) {
            safeNotes = "";
    }  else {
        safeNotes = notes;
    } 
        return new DrillEntry(id, sessionId, drillType, reps, success, safeNotes);
    }
    public String neat() {
        StringBuilder sb = new StringBuilder();

        sb.append("Drill: ").append(drillType).append(" | Reps: ").append(reps);

        if (success > 0) {
            sb.append(" | Success: ").append(success);
        }

        if (!notes.isBlank()) {
            sb.append(" | Notes: ").append(notes);
        }

        sb.append(" | ID: ").append(id);

        return sb.toString();
        
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
