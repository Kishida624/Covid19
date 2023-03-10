package kis.covid19;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author naoki
 */
public class CreateData {
    @Data
    @NoArgsConstructor
    static class Pref {
        String pref;
        int patients;
        int hospitalizations;
        int discharges;
        int mortality;
        int pcr;
        int severe;

        public Pref(String pref, int patients, int hospitalizations, int discharges, int mortality, int severe, int pcr) {
            this.pref = pref;
            this.patients = patients;
            this.hospitalizations = hospitalizations;
            this.discharges = discharges;
            this.mortality = mortality;
            this.severe = severe;
            this.pcr = pcr;
        }
        
        public Pref(String pref, int patients, int hospitalizations, int discharges, int mortality) {
            this(pref, patients, hospitalizations, discharges, mortality, 0, 0);
        }
        public Pref(String pref, String patients, String hospitalizations, String discharges, String mortality) {
            this(pref, Util.parseInt(patients), Util.parseInt(hospitalizations), Util.parseInt(discharges), Util.parseInt(mortality));
        }
    }
    
    @Data
    static class InputPref {
        String lastupdate;
        List<Pref> prefList;
    }
    
    @Data
    static class Prefs {
        String lastUpdate;
        List<ChartPref> prefs = new ArrayList<>();
    }
    @Data
    @NoArgsConstructor
    static class ChartPref {
        int code;
        String pref;
        int population;
        List<Integer> patients;
        List<Integer> motarity;
        List<Integer> hospitalizations;
        List<Integer> severes;
        List<String> dates;
        
        ChartPref(String code, String pref, String population) {
            this.code = Integer.parseInt(code.trim());
            this.pref = pref.trim();
            this.population = Integer.parseInt(population.trim());
            patients = new ArrayList<>();
            dates = new ArrayList<>();
            motarity = new ArrayList<>();
            hospitalizations = new ArrayList<>();
            severes = new ArrayList<>();
        }
    }
    
    public static void main(String[] args) throws IOException {
        Prefs prefs = new Prefs();
        prefs.prefs = prefString.lines()
                .map(p -> p.split(","))
                .map(p -> new ChartPref(p[0], p[1], p[2]))
                .collect(Collectors.toUnmodifiableList());
        var start = LocalDate.of(2022,6,1);
        //var start = LocalDate.of(2020,3,8);
        var tokyoDif = TokyoDiff.dateDiff();
        Stream.concat(
                Stream.iterate(start, d -> d.plusDays(1))
                      .map(d -> Path.of(PrefJsonProc.jsonName(d)))
                      .takeWhile(Files::exists),
                // Stream.of(Path.of("data/nhk/input-pref2020-04-03.json")))
                Stream.empty())
              .forEach(path -> {
                    InputPref data = PrefJsonProc.readJson(path);
                    var pat = Pattern.compile("(\\d+)-(\\d{1,2})[/-](\\d{1,2})");
                    var mat = pat.matcher(data.lastupdate);
                    if (!mat.find()) {
                        throw new IllegalArgumentException("wrong date for " + data.lastupdate);
                    }
                    LocalDate date;
                    try {
                        date = LocalDate.of(Integer.parseInt(mat.group(1)), 
                                Integer.parseInt(mat.group(2)),
                                Integer.parseInt(mat.group(3)));
                    } catch (DateTimeException ex) {
                        throw new RuntimeException("wrong date for " + data.lastupdate);
                    }
                    Map<String, Pref> patients = data.prefList.stream()
                            .collect(Collectors.toUnmodifiableMap(Pref::getPref, Function.identity()));
                    Pref zero = new Pref();
                    for (var p : prefs.prefs) {
                        p.dates.add(date.toString());
                        var pref = patients.getOrDefault(p.pref, zero);
                        if (p.pref.equals("?????????")) {
                            var dt = date.minusDays(1);
                            if (tokyoDif.containsKey(dt)) {
                                p.patients.add(tokyoDif.get(dt).totalFixed());
                            } else if (dt.isAfter(LocalDate.of(2021, 10, 2)) && dt.isBefore(LocalDate.of(2021, 10, 29))) {
                                p.patients.add(pref.patients + 4512 - 447);
                            } else {
                                p.patients.add(pref.patients);
                            }
                        } else {
                            p.patients.add(pref.patients);
                        }
                        p.motarity.add(pref.mortality);
                        p.hospitalizations.add(Math.max(pref.hospitalizations, 0));
                        p.severes.add(pref.severe);
                    }
                    prefs.lastUpdate = date.toString();
              });
        
        var mapper = PrefJsonProc.createMapper();
        try (var bw = Files.newBufferedWriter(Path.of("docs/prefs.js"));
             var pw = new PrintWriter(bw)) {
            pw.printf("let data = %s;%n", mapper.writeValueAsString(prefs));
        }
    }
    
        
    
    static String prefString = "" +
        " 1,?????????  , 5320\n" +
        " 2,?????????  , 1278\n" +
        " 3,?????????  , 1255\n" +
        " 4,?????????  , 2323\n" +
        " 5,?????????  ,  996\n" +
        " 6,?????????  , 1102\n" +
        " 7,?????????  , 1882\n" +
        " 8,?????????  , 2892\n" +
        " 9,?????????  , 1957\n" +
        "10,?????????  , 1960\n" +
        "11,?????????  , 7310\n" +
        "12,?????????  , 6246\n" +
        "13,?????????  ,13724\n" +
        "14,????????????, 9159\n" +
        "15,?????????  , 2267\n" +
        "16,?????????  , 1056\n" +
        "17,?????????  , 1147\n" +
        "18,?????????  ,  779\n" +
        "19,?????????  ,  823\n" +
        "20,?????????  , 2076\n" +
        "21,?????????  , 2008\n" +
        "22,?????????  , 3675\n" +
        "23,?????????  , 7525\n" +
        "24,?????????  , 1800\n" +
        "25,?????????  , 1413\n" +
        "26,?????????  , 2599\n" +
        "27,?????????  , 8823\n" +
        "28,?????????  , 5503\n" +
        "29,?????????  , 1348\n" +
        "30,????????????,  945\n" +
        "31,?????????  ,  565\n" +
        "32,?????????  ,  685\n" +
        "33,?????????  , 1907\n" +
        "34,?????????  , 2829\n" +
        "35,?????????  , 1383\n" +
        "36,?????????  ,  743\n" +
        "37,?????????  ,  967\n" +
        "38,?????????  , 1364\n" +
        "39,?????????  ,  714\n" +
        "40,?????????  , 5107\n" +
        "41,?????????  ,  824\n" +
        "42,?????????  , 1354\n" +
        "43,?????????  , 1765\n" +
        "44,?????????  , 1152\n" +
        "45,?????????  , 1089\n" +
        "46,????????????, 1626\n" +
        "47,?????????  , 1443\n" +
        "";
}
