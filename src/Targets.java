public record Targets(
    int sessionsPerWeek,
    int wallBallRepsPerWeek,
    int gymMinutesPerWeek
) {
    public static Targets defaultTargets() {
        return new Targets(4, 1000, 120);
    }

    public String toCsv() {
        return sessionsPerWeek + "," +
            wallBallRepsPerWeek + "," +
            gymMinutesPerWeek;
    }

    public static Targets fromCsv(String line) {
        var cols = CsvUtil.parse(line);
        if (cols.size() < 3) throw new IllegalArgumentException("Bad targets row: " + line);

        return new Targets(
            Integer.parseInt(cols.get(0)),
            Integer.parseInt(cols.get(1)),
            Integer.parseInt(cols.get(2))
        );
    }

}
