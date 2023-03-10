package kis.covid19;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * @author naoki
 */
public class Util {
    public static int parseInt(String text) {
        if (text.equals("-")) {
            return 0;
        }
        return Integer.parseInt(text.replaceAll(",", ""));
    }
    
    public static String zenDigitToHan(String text) {
        var zen = "０１２３４５６７８９";
        var han = "0123456789";
        String result = text;
        for (int i = 0; i < zen.length(); ++i) {
            result = result.replaceAll(zen.substring(i, i + 1), han.substring(i, i + 1));
        }
        return result;
    }

    static String stripSpace(String str) {
        return str.replaceAll("[ 　]+", "");
    }
    
    static LocalDate readReiwaDate(String text) {
        var pt = Pattern.compile("(\\d+)年(\\d+)月(\\d+)日");
        String normtext = zenDigitToHan(stripSpace(text));
        var mat = pt.matcher(normtext);
        if (mat.find()) {
            var dt = JapaneseDate.of(JapaneseEra.REIWA, 
                    Integer.parseInt(mat.group(1)),
                    Integer.parseInt(mat.group(2)),
                    Integer.parseInt(mat.group(3)));
            return LocalDate.from(dt);
        } else {
            var ptg = Pattern.compile("(\\d{4})/(\\d{1,2})/(\\d{1,2})");
            var matg = ptg.matcher(normtext);
            if (matg.find()) {
                return LocalDate.of(
                        Integer.parseInt(matg.group(1)),
                        Integer.parseInt(matg.group(2)),
                        Integer.parseInt(matg.group(3)));
            }else if (true) {
                throw new IllegalArgumentException("Can't find a date");
            } else {
                System.out.println("can't find a date");
                System.out.println(text);
                return LocalDate.of(2021, 9, 30);
            }
        }
    }
    
    public static String readPdf(String url) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        try (var is = res.body(); 
             var doc = PDDocument.load(is)) {
            var stripper = new PDFTextStripper();
            var pageCount = doc.getNumberOfPages();
            if (pageCount == 3) {
                stripper.setStartPage(2);
                stripper.setEndPage(2);
            }
            var text = stripper.getText(doc);
            return text;
        }
    }
    
    public static String addPrefSuffix(String pref) {
        return CreateData.prefString.lines()
                .map(line -> line.split(",")[1])
                .map(String::trim)
                .filter(p -> p.startsWith(pref))
                .findFirst()
                .orElse(pref);
    }
}
