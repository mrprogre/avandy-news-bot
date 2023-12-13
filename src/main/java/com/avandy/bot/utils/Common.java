package com.avandy.bot.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@UtilityClass
public class Common {
    public static final String ERROR_TEXT = "Error occurred: ";

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

    // Input: 2023-12-13 13:39:06.0
    // Output: 13:39 13.12
    public static String dateToShowFormatChange(String input) {
        return input.substring(11, 16) + " " + input.substring(8, 10) + "." + input.substring(5, 7);
    }

    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
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

}
