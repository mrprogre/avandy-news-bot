package com.avandy.bot.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@UtilityClass
public class Common {
    public static final String ERROR_TEXT = "Error occurred: ";
    public static final String ICON_SEARCH = "\uD83C\uDFB2";
    public static final String ICON_NEWS_FOUNDED = "\uD83C\uDF3F";
    public static final String ICON_END_SEARCH_NOT_FOUND = "\uD83D\uDCA4";
    public static final String ICON_GOOD_BYE = "\uD83D\uDC4B";
    public static final String ICON_SETTINGS = "\uD83C\uDF0D";
    public static final String ICON_TOP_20_FIRE = "\uD83D\uDD25";

    // Compare dates to find news by period
    public static int compareDates(Date now, Date in, int minutes) {
        Calendar minus = Calendar.getInstance();
        minus.setTime(new Date());
        minus.add(Calendar.MINUTE, -minutes);
        Calendar now_cal = Calendar.getInstance();
        now_cal.setTime(now);

        if (in.after(minus.getTime()) && in.before(now_cal.getTime())) {
            return 1;
        } else
            return 0;
    }

    public static String getHash(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5"); // MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 10);
        } catch (Exception e) {
            log.error(Common.ERROR_TEXT + e.getMessage());
        }
        return null;
    }

    public static int timeMapper(String time) {
        time = time.toLowerCase();
        try {
            int minutes = Integer.parseInt(time.replaceAll("\\D+", ""));

            if (time.contains("h")) {
                return minutes * 60;
            } else if (time.contains("m")) {
                return minutes;
            } else if (time.contains("s")) {
                return (int) Math.ceil((double) minutes / 60);
            } else {
                return 1440;
            }
        } catch (Exception e) {
            return 1440;
        }
    }

    // INPUT  2023-12-13 13:39:06.0
    // OUTPUT 13:39 13.12
    public static String dateToShowFormatChange(String input) {
        return input.substring(11, 16) + " " + input.substring(8, 10) + "." + input.substring(5, 7);
    }

    public static List<Integer> getTimeToExecute(LocalTime start, String interval) {
        List<Integer> times = new ArrayList<>();
        int intervalInt;
        if (interval.equals("48h") || interval.equals("72h")) return List.of(start.getHour());

        if (interval.contains("h")) {
            intervalInt = Integer.parseInt(interval.replace("h", ""));
            int iterations = 24 / intervalInt;
            LocalTime currentTime = start;
            for (int i = 0; i < iterations; i++) {
                currentTime = currentTime.plusHours(intervalInt);
                times.add(currentTime.getHour());
            }
        }
        times.sort(Integer::compare);
        return times;
    }

    // поиск общей подстроки (полно всяких: ный, ять, акой, ать)
    private String longestCommonSubstring(String s, String t) {
        int[][] table = new int[s.length()][t.length()];
        int longest = 0;
        String result = "";
        for (int i = 0; i < s.length(); i++) {
            for (int j = 0; j < t.length(); j++) {
                if (s.charAt(i) != t.charAt(j)) {
                    continue;
                }
                table[i][j] = (i == 0 || j == 0) ? 1
                        : 1 + table[i - 1][j - 1];
                if (table[i][j] > longest) {
                    longest = table[i][j];
                }
                if (table[i][j] == longest) {
                    result = s.substring(i - longest + 1, i + 1);
                }
            }
        }
        return result;
    }

    // Объединяет и суммирует однокоренные слова c разными окончаниями методом Jaro-Winkler Distance
    // input: атака-1, атаку-2, атаке-3, атакован-4
    // output: level < 87 : атак-10
    // output: level = 87 : атак-5 [атаку-2, атаке-3]
    // output: level > 87 : null
    public Map<String, Integer> fillTopWithoutDuplicates(Map<String, Integer> wordsCount, int jaroWinklerLevel) {

        Map<String, Integer> topMap = new TreeMap<>();
        List<String> forDeleteFromMap = new ArrayList<>();
        List<String> excluded = new ArrayList<>();

        for (Map.Entry<String, Integer> word1 : wordsCount.entrySet()) {
            for (Map.Entry<String, Integer> word2 : wordsCount.entrySet()) {
                int compare = new JaroWinklerDistance().compare(word1.getKey(), word2.getKey());

                if (compare != 100 && compare >= jaroWinklerLevel && !excluded.contains(word1.getKey())) {
                    String string = longestCommonSubstring(word1.getKey(), word2.getKey());
                    if (string.length() > 3) {
                        forDeleteFromMap.add(word1.getKey());
                        forDeleteFromMap.add(word2.getKey());
                        topMap.put(string, topMap.getOrDefault(string, 0) + word1.getValue());
                    }
                    excluded.add(word1.getKey());
                }
            }
        }

        // Удаление однокоренных слов из общей коллекции
        for (String word : forDeleteFromMap) {
            wordsCount.remove(word);
        }

        // добавление обобщённого слова в общую коллекцию слов
        wordsCount.putAll(topMap);

        return wordsCount;
    }

}
