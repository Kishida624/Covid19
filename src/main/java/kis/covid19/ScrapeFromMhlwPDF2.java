package kis.covid19;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import kis.covid19.CreateData.Pref;
import org.jsoup.Jsoup;

/**
 * for data since 5/9
 * @author naoki
 */
public class ScrapeFromMhlwPDF2 {
    public static void main(String[] args) throws IOException, InterruptedException {
        //var url = "https://www.mhlw.go.jp/content/10906000/000628667.pdf";
        //System.out.println(Util.readPdf(url));
        // System.out.println(getPDFUrl("https://www.mhlw.go.jp/stf/newpage_12250.html"));
        scrape("https://www.mhlw.go.jp/content/10906000/000837763.pdf");
    }

    static String getPDFUrl(String url) throws IOException {
        var conn = Jsoup.connect(url);
        var doc = conn.get();
        return "https://www.mhlw.go.jp" + doc.select("a").stream()
                .filter(elm -> elm.text().contains("各都道府県"))
                .findFirst().orElseThrow()
                .attr("href");
    }
    
    static void scrape(String url) throws IOException, InterruptedException {
        var text = convertStranges(Util.readPdf(url));
        dataToJson(text);
    }
    
    static void dataToJson(String text) throws IOException {
        var data = readData(text);
        var date = Util.readReiwaDate(text).plusDays(1);
        PrefJsonProc.writeJson(date, data);
    }

    static String convertStranges(String text) {
        char[] strange = {'\u2ED8', '\u2F2D', '\u2ED1', '\u2FC5', '\u2F49', '\u2F47'};
        char[] normal = {'\u9752', '\u5C71', '\u9577', '\u9E7F', '\u6708', '\u65E5'};
        var str = new StringBuilder(text);
        for (int i = 0; i < text.length(); ++i) {
            for (int j = 0; j < strange.length; ++j) {
                if (str.charAt(i) == strange[j]) {
                    
                    str.setCharAt(i, normal[j]);
                    break;
                }
            }
        }
        return str.toString();
    }
    
    static List<Pref> readData(String text) {
        return Util.zenDigitToHan(text).replaceAll("※\\d+", "").lines()
                .map(ScrapeFromMhlwPDF2::parsePref)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }
    
    static Pref parsePref(String line) {
        String[] arr = parseLine(line);
        if (arr.length < 8) {
            return null;
        }
        String pref = Util.addPrefSuffix(arr[0]);
        int patient = Util.parseInt(arr[1]);
        int pcr = Util.parseInt(arr[2]);
        int hospitalizations = Util.parseInt(arr[3]);
        int severe = Util.parseInt(arr[4]);
        int discharge = Util.parseInt(arr[5]);
        int mortarity = Util.parseInt(arr[6]);
        int confirming = Util.parseInt(arr[7]);
        
        return new Pref(pref, patient, patient - discharge - mortarity, discharge, mortarity, severe, pcr);
    }
    
    static String[] parseLine(String line) {
        var conv = removeSpaceFromPref(line.replaceAll("　", " ").trim())
            .replaceAll("不明", "0")
            .replaceAll(",", "");
        return conv.split("\\s+");
    }
    
    static String removeSpaceFromPref(String line) {
        line = line.replaceAll("　", " ");
        var p = Pattern.compile("^\\D(\\s*\\D)+");
        var mat = p.matcher(line);
        if (!mat.find()) {
            return line;
        }
        var f = mat.group(0).trim();
        var rf = f.replaceAll("\\s+", "").replace("?", "長").replace("⾧", "長");

        return line.replace(f, rf);
    }
}
