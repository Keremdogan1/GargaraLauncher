import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

public class Extractor {
    public static void main(String[] args) throws Exception {
        String html = new String(Files.readAllBytes(Paths.get("C:\\Users\\kerem\\.gemini\\antigravity\\brain\\bb2928d9-4c5f-4e9b-a27d-97895937a12c\\.system_generated\\steps\\875\\content.md")), "UTF-8");
        Matcher m = Pattern.compile("entry\\.\\d+").matcher(html);
        Set<String> entries = new HashSet<>();
        while(m.find()) {
            entries.add(m.group());
        }
        for(String entry : entries) {
            System.out.println(entry);
        }
    }
}
