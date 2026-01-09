import java.util.Arrays;
import java.util.List;

public class CsvUtil {
    public static List<String> parse(String line) {
        return Arrays.asList(line.split(",", -1));
    }

    public static String clean(String s) {
        if (s == null) return "";
        return s.replace(",", " ");
    }

}
