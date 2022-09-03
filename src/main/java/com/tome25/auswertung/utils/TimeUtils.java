package com.tome25.auswertung.utils;

import java.util.Calendar;
import java.util.Objects;

/**
 * A class containing utility methods related to time stamp, time, and date
 * handling.
 * 
 * @author theodor
 */
public class TimeUtils {

	/**
	 * Converts the given time in the format "HH:MM:SS.2" to time of day in
	 * milliseconds.<br/>
	 * Hours do not have to be two digits, they can be 1+.<br/>
	 * Minutes and seconds can be 1 or 2 digits.<br/>
	 * Seconds can have 0 to 2 decimal digits.
	 * 
	 * @param time The time to convert.
	 * @return The parsed time.
	 * @throws NullPointerException     If {@code time} is {@code null}.
	 * @throws IllegalArgumentException If {@code time} does not match the required
	 *                                  input format("HH:MM:SS.2").<br/>
	 *                                  Or if part of the time string is not contain
	 *                                  a parsable integer.
	 */
	public static long parseTime(String time) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(time, "The time to parse cannot be null.");

		long result = 0;
		String split[] = time.split(":");

		if (split.length != 3) {
			throw new IllegalArgumentException("Time string \"" + time + "\" does not match format.");
		}

		result += Integer.parseInt(split[0]) * 3600000l; // milliseconds per hour
		result += Integer.parseInt(split[1]) * 60000l; // milliseconds per minute

		String seconds_split[] = split[2].split("\\.");

		if (seconds_split.length == 0 || seconds_split.length > 2) {
			throw new IllegalArgumentException("Time string \"" + time + "\" does not match format.");
		}

		result += Integer.parseInt(seconds_split[0]) * 1000;

		if (seconds_split.length == 2) {
			if (seconds_split[1].length() > 2) {
				throw new IllegalArgumentException("Hundredths part of time stamp can't be over 99.");
			} else if (seconds_split[1].length() == 2) {
				result += Integer.parseInt(seconds_split[1]) * 10;
			} else if (seconds_split[1].length() == 1) {
				result += Integer.parseInt(seconds_split[1]) * 100; // seconds with one decimal digit have tenths, not
																	// hundredths.
			}
		}

		return result;
	}

	/**
	 * Returns the string representation of the given time in milliseconds.<br/>
	 * The format is "HH:MM:SS.2".
	 * 
	 * @param time The time in miliseconds to convert.
	 * @return The string representation of the given time.
	 * @throws IllegalArgumentException If {@code time} is less than 0.
	 */
	public static String encodeTime(long time) throws IllegalArgumentException {
		if (time < 0) {
			throw new IllegalArgumentException("Time to encode can't be negative.");
		}

		int hours = (int) (time / 3600000); // 60 minutes * 60 seconds * 1000 ms
		int minutes = (int) (time % 3600000 / 60000);
		int seconds = (int) (time % 60000 / 1000);
		int hundredths = (int) (time % 1000 / 10);

		return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths);
	}

	/**
	 * Parses the given date string and converts it to a {@link Calendar}
	 * representing the encoded time.
	 * 
	 * @param date The date to parse.
	 * @return The parsed date.
	 * @throws NullPointerException     If {@code date} is {@code null}.
	 * @throws IllegalArgumentException If {@code date} does not match the format
	 *                                  "DD.MM.YYYY".
	 */
	public static Calendar parseDate(String date) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(date, "The date to parse can't be null.");

		String dateSplit[] = date.split("\\.");
		if (dateSplit.length != 3) {
			throw new IllegalArgumentException("Date string \"" + date + "\" does not match required format.");
		}

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(0);
		c.set(Integer.parseInt(dateSplit[2]), Integer.parseInt(dateSplit[1]) - 1, Integer.parseInt(dateSplit[0]));

		return c;
	}

	/**
	 * Converts the given {@link Calendar} object to a date string of the format
	 * "DD.MM.YYYY".
	 * 
	 * @param date The date to convert.
	 * @return The date string representing the given date.
	 * @throws NullPointerException If {@code date} is {@code null}.
	 */
	public static String encodeDate(Calendar date) throws NullPointerException {
		Objects.requireNonNull(date, "The date to encode can't be null.");

		return String.format("%02d.%02d.%04d", date.get(Calendar.DATE), date.get(Calendar.MONTH) + 1,
				date.get(Calendar.YEAR));
	}

	/**
	 * Checks whether {@code second} is exactly one day after {@code first}.
	 * 
	 * @param first  The first of the days to compare.
	 * @param second The second of the days to compare.
	 * @return {@code true} if {@code second} is the day after {@code first}.
	 * @throws NullPointerException     If {@code first} or {@code second} is
	 *                                  {@code null}.
	 * @throws IllegalArgumentException If one of the dates doesn't match the format
	 *                                  "DD.MM.YYYY".
	 */
	public static boolean isNextDay(String first, String second) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(first, "The first day to check can't be null.");
		Objects.requireNonNull(second, "The second day to check can't be null.");

		Calendar c1 = parseDate(first);
		Calendar c2 = parseDate(second);
		c1.add(Calendar.DATE, 1);

		return c1.equals(c2);
	}
}
