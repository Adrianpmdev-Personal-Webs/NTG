package com.adrianpmdev.wowforever.features.gamedata.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How game tooltips write numbers and durations.
 *
 * @author adrianpmdev
 */
public final class TooltipFormat {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int DEFAULT_DECIMALS = 2;
    private static final int THOUSANDS = 3;
    private static final double EPSILON = 1e-6;

    private TooltipFormat() {
    }

    /** Whole numbers without decimals, others with up to two: 15, 0.5, 1.25. */
    public static String number(double value) {
        double rounded = Math.round(value);

        if (Math.abs(value - rounded) < EPSILON) {
            return Long.toString((long) rounded);
        }

        return BigDecimal.valueOf(value).setScale(DEFAULT_DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros()
                .toPlainString();
    }

    /** A number inside a text: as {@link #number(double)}, with thousands separated: 1,200, 0.5. */
    public static String text(double value) {
        return grouped(number(value));
    }

    /** A number inside a text rounded to a whole number, with thousands separated: 17.25 reads 17, 1200 reads 1,200. */
    public static String whole(double value) {
        return grouped(Long.toString(Math.round(value)));
    }

    /** A fixed number of decimals, as a formula followed by {@code .1} asks: 1.0, 2.5. */
    public static String number(double value, int decimals) {
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * A duration in the largest unit that writes it with at most one decimal: "15 sec", "1.5 min", "2 min", "1 hr",
     * "2 hrs", and "until cancelled" for a negative one.
     */
    public static String duration(double seconds) {
        if (seconds < 0) {
            return "until cancelled";
        }

        if (seconds >= SECONDS_PER_HOUR && oneDecimal(seconds / SECONDS_PER_HOUR)) {
            double hours = seconds / SECONDS_PER_HOUR;

            return number(hours) + (hours == 1 ? " hr" : " hrs");
        }

        if (seconds >= SECONDS_PER_MINUTE && oneDecimal(seconds / SECONDS_PER_MINUTE)) {
            return number(seconds / SECONDS_PER_MINUTE) + " min";
        }

        return number(seconds) + " sec";
    }

    private static boolean oneDecimal(double value) {
        double tenths = value * 10;

        return Math.abs(tenths - Math.round(tenths)) < EPSILON;
    }

    /** Separates the thousands of the whole part of a number: 12000.5 reads 12,000.5. */
    private static String grouped(String number) {
        boolean negative = number.startsWith("-");
        String digits = negative ? number.substring(1) : number;
        int point = digits.indexOf('.');
        String whole = point < 0 ? digits : digits.substring(0, point);
        StringBuilder grouped = new StringBuilder();

        for (int index = 0; index < whole.length(); index++) {
            if (index > 0 && (whole.length() - index) % THOUSANDS == 0) {
                grouped.append(',');
            }

            grouped.append(whole.charAt(index));
        }

        return (negative ? "-" : "") + grouped + (point < 0 ? "" : digits.substring(point));
    }
}
