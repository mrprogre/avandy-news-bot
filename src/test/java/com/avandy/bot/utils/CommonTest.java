package com.avandy.bot.utils;

import com.avandy.bot.model.Headline;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CommonTest {
    private final JaroWinklerDistance jwd = new JaroWinklerDistance();

    @Test
    public void checkJaroWinkler() {
        String s1 = "Путин поделился впечатлениями от поездки на снегоболотоходе";
        String s2 = "Путин поделился впечатлениями от поездки на вездеходе по Чукотке";
        System.out.println(jwd.compare(s1, s2));
    }

    @Test
    public void checkJaroWinklerDistanceCompare() {
        String s1 = "CRATE", s2 = "TRACE", s3 = "DwAyNE", s4 = "DuANE";
        assertEquals(73, jwd.compare(s1, s2));
        assertEquals(82, jwd.compare(s3, s4));

        s1 = "атака";
        s2 = "атаке";
        s3 = "атаку";
        s4 = "атакован";
        assertEquals(86, jwd.compare(s1, s2));
        assertEquals(86, jwd.compare(s1, s3));
        assertEquals(87, jwd.compare(s1, s4));
        assertEquals(86, jwd.compare(s2, s3));
        assertEquals(76, jwd.compare(s2, s4));
        assertEquals(76, jwd.compare(s3, s4));

        s1 = "played";
        s2 = "player";
        assertEquals(88, jwd.compare(s1, s2));
    }

    @Test
    public void timeMapperTest() {
        assertEquals(Common.timeMapper("2h"), 120, "1");
        assertEquals(Common.timeMapper("2H"), 120, "2");
        assertEquals(Common.timeMapper("2-h"), 120, "3");
        assertEquals(Common.timeMapper("20m"), 20, "4");
        assertEquals(Common.timeMapper("61s"), 2, "5");
        assertEquals(Common.timeMapper("-12ffaaff"), 1440, "6");
    }

    @Test
    public void getHashTest() {
        assertEquals(Common.getHash("Новость" + 2), Common.getHash("Новость" + 2));
        assertEquals(Common.getHash("Новость" + 4), Common.getHash("Новость" + 4));
        assertNotEquals(Common.getHash("Новость" + 2), Common.getHash("Новость" + 4));
    }

    @Test
    public void dateToShowFormatChangeTest() {
        assertEquals(Common.dateToShowFormatChange("2023-12-13 14:39:06.0"), "14:39 13.12");
    }

    @Test
    public void getTimeToExecuteTest() {
        LocalTime start = LocalTime.of(19, 0);
        System.out.println("1h " + Common.getTimeToExecute(start, "1h"));
        System.out.println("2h " + Common.getTimeToExecute(start, "2h"));
        System.out.println("4h " + Common.getTimeToExecute(start, "4h"));
        System.out.println("12h " + Common.getTimeToExecute(start, "12h"));
        System.out.println("24h " + Common.getTimeToExecute(start, "24h"));
        System.out.println("48h " + Common.getTimeToExecute(start, "48h"));
        System.out.println("72h " + Common.getTimeToExecute(start, "72h"));
    }

    @Test
    public void shouldReturnTwoUniqueTitles() {
        Set<Headline> headlinesToShow = new HashSet<>();
        headlinesToShow.add(new Headline("111", "Росимущество получило в прямое распоряжение 59,8% акций ТГК-2",
                "link1",
                new Date(), 1L, 1, "hash1"));
        headlinesToShow.add(new Headline("222", "Росимущество получило в прямое распоряжение 59,8% акций ТГК-2",
                "link2",
                new Date(), 1L, 1, "hash1"));
        headlinesToShow.add(new Headline("333", "Росимущество получило в прямое распоряжение 59,8% акций ТГК-2",
                "link2",
                new Date(), 1L, 1, "hash1"));
        headlinesToShow.add(new Headline("444", "Росимущество получило в прямое распоряжение 59,8% акций ТГК-2",
                "link2",
                new Date(), 1L, 1, "hash1"));
        headlinesToShow.add(new Headline("555", "Отличающаяся информация",
                "link3",
                new Date(), 1L, 3, "hash3"));

        List<Headline> headlinesDeleteJw = new ArrayList<>();
        headlinesToShow.parallelStream().forEach(headline1 -> headlinesToShow.forEach(headline2 -> {
            int compare = jwd.compare(headline1.getTitle(), headline2.getTitle());

            if (compare >= 85 && compare != 100) {
                headlinesDeleteJw.add(headline1);
                headlinesDeleteJw.remove(headline2);
            }
        }));

        List<Headline> collect = headlinesDeleteJw.stream().distinct().toList();
        collect.forEach(System.out::println);
        System.out.println("--------------------");
        collect.forEach(headlinesToShow::remove);
        headlinesToShow.forEach(System.out::println);
        assertEquals(2, headlinesToShow.size());
    }
}
