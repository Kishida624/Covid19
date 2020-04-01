package kis.covid19;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 *
 * @author naoki
 */
public class ScrapeFromMhlwPDF {
    public static void main(String[] args) throws IOException, InterruptedException {
        // var url = "https://www.mhlw.go.jp/content/10906000/000605161.pdf"; // 3/8 (patient only)
        // var url = "https://www.mhlw.go.jp/content/10906000/000608410.pdf"; // 3/14
        // var url = "https://www.mhlw.go.jp/content/10906000/000608453.pdf"; // 3/15
        // var url = "https://www.mhlw.go.jp/content/10906000/000610652.pdf"; // 3/20 with detail
        // var url = "https://www.mhlw.go.jp/content/10906000/000610713.pdf"; // 3/21
        // var url = "https://www.mhlw.go.jp/content/10906000/000610761.pdf"; // 3/22
        // var url = "https://www.mhlw.go.jp/content/10906000/000614789.pdf"; // 3/28
        var url = "https://www.mhlw.go.jp/content/10906000/000615354.pdf"; // 3/29
        var date = LocalDate.of(2020, 3, 29);
        var client = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();
        var res = client.send(req, BodyHandlers.ofInputStream());
        try (var is = res.body();
             var doc = PDDocument.load(is)) {
            var stripper = new PDFTextStripper();
            var text = stripper.getText(doc);
            var pat = Pattern.compile("(\\S+)\\s+(\\d+)\\s*名?");
            Map<String, Integer> data = text.lines()
                    .map(pat::matcher)
                    .filter(Matcher::find)
                    .collect(Collectors.toUnmodifiableMap(mat -> mat.group(1),
                            mat -> Integer.parseInt(mat.group(2))));
            PrefJsonProc.writeJson(date, data);
        }
    }
}
