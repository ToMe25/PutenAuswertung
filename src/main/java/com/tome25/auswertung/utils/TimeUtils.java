package com.tome25.auswertung.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.TimeZone;

/**
 * A class containing utility methods related to time stamp, time, and date
 * handling.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class TimeUtils {

	/**
	 * The number of milliseconds in a day.
	 */
	public static final int DAY_MS = 24 * 60 * 60 * 1000;

	/**
	 * The minimum number of digits a year number should have.<br/>
	 * A year is prefixed with zeros until its long enough.
	 */
	public static final short YEAR_MIN_DIGITS = 4;

	/**
	 * The character to be used to separate the integer part from the fractional
	 * part of a decimal.
	 */
	private static volatile char decimal_separator = '.';

	/**
	 * Converts the given time in the format "HH:MM:SS.2" to time of day in
	 * milliseconds.<br/>
	 * Hours do not have to be two digits, they can be one or more.<br/>
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

		String seconds_split[] = split[2].split("[\\.,]");

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
		return parseTime(date, 0);
	}

	/**
	 * Parses a {@link Calendar} object from the given date string and time of day
	 * in milliseconds.
	 * 
	 * @param date The date for the {@link Calendar} object.
	 * @param time The time of day in milliseconds.
	 * @return A {@link Calendar} object representing the given time.
	 * @throws NullPointerException     If {@code date} is {@code null}.
	 * @throws IllegalArgumentException If {@code date} does not match the format
	 *                                  "DD.MM.YYYY".<br/>
	 *                                  Or if {@code time} is negative.
	 */
	public static Calendar parseTime(String date, int time) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(date, "The date to parse can't be null.");

		// Prevents a whole bunch of issues.
		// Placing this here calls it way too often, but makes unit tests easier.
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

		if (time < 0) {
			throw new IllegalArgumentException("Time of day can't be negative.");
		}

		String dateSplit[] = date.split("\\.");
		if (dateSplit.length != 3) {
			throw new IllegalArgumentException("Date string \"" + date + "\" does not match required format.");
		}

		if (dateSplit[0].isEmpty()) {
			throw new IllegalArgumentException("Date string \"" + date + "\" has an empty day component.");
		} else if (dateSplit[1].isEmpty()) {
			throw new IllegalArgumentException("Date string \"" + date + "\" has an empty month component.");
		} else if (dateSplit[2].isEmpty()) {
			throw new IllegalArgumentException("Date string \"" + date + "\" has an empty year component.");
		}

		Calendar c = new GregorianCalendar();
		c.setTimeZone(TimeZone.getTimeZone("GMT"));
		c.setTimeInMillis(time);
		c.set(Calendar.YEAR, Integer.parseInt(dateSplit[2]));

		int month = Integer.parseInt(dateSplit[1]) - 1;
		if (month > c.getActualMaximum(Calendar.MONTH)) {
			throw new IllegalArgumentException("The month of date \"" + date + "\" is too large.");
		} else {
			c.set(Calendar.MONTH, month);
		}

		int day = Integer.parseInt(dateSplit[0]);
		if (day > c.getActualMaximum(Calendar.DATE)) {
			throw new IllegalArgumentException("The day of date \"" + date + "\" is too large.");
		} else {
			c.set(Calendar.DATE, day);
		}

		c.add(Calendar.DATE, time / DAY_MS);

		return c;
	}

	/**
	 * Parses a {@link Calendar} object from the given date string and time of day.
	 * 
	 * @param date The date for the {@link Calendar} object.
	 * @param time The time of day for the {@link Calendar}.<br/>
	 *             Cannot be more than 24 hours.<br/>
	 *             Exactly 24 hours is allowed because of rounding.
	 * @return A {@link Calendar} object representing the given time.
	 * @throws NullPointerException     If {@code date} or {@code time} is
	 *                                  {@code null}.
	 * @throws IllegalArgumentException If one of the strings doesn't match the
	 *                                  required format.<br/>
	 *                                  Or time is more than 24 hours.
	 */
	public static Calendar parseTime(String date, String time) throws NullPointerException, IllegalArgumentException {
		long ms = parseTime(time);
		if (ms > DAY_MS) {
			throw new IllegalArgumentException("The time of day to parse has to be less than a full day.");
		}
		return parseTime(date, (int) ms);
	}

	/**
	 * Returns the string representation of the given time in milliseconds.<br/>
	 * The format is "HH:MM:SS.2".
	 * 
	 * @param time The time in milliseconds to convert.
	 * @return The string representation of the given time.
	 * @throws IllegalArgumentException If {@code time} is less than 0.
	 */
	public static String encodeTime(long time) throws IllegalArgumentException {
		if (time < 0) {
			throw new IllegalArgumentException("Time to encode can't be negative.");
		}

		if (time % 10 != 0) {
			time = ((time + 5) / 10) * 10; // round to next ten
		}

		int hours = (int) (time / 3600000); // 60 minutes * 60 seconds * 1000 ms
		int minutes = (int) (time % 3600000 / 60000);
		int seconds = (int) (time % 60000 / 1000);
		int hundredths = (int) (time % 1000 / 10);

		StringBuilder result = new StringBuilder();
		if (hours < 10) {
			result.append('0');
		}
		result.append(hours);
		result.append(':');

		if (minutes < 10) {
			result.append('0');
		}
		result.append(minutes);
		result.append(':');

		if (seconds < 10) {
			result.append('0');
		}
		result.append(seconds);
		result.append(getDecimalSeparator());

		if (hundredths < 10) {
			result.append('0');
		}
		result.append(hundredths);

		return result.toString();
	}

	/**
	 * Converts the given time to a date string of the format "DD.MM.YYYY".
	 * 
	 * @param time The time to encode.
	 * @return The date string representing the given time.
	 * @throws NullPointerException If {@code date} is {@code null}.
	 */
	public static String encodeDate(long time) {
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(time);
		return encodeDate(cal);
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

		StringBuilder result = new StringBuilder();
		int day = date.get(Calendar.DATE);
		if (day < 10) {
			result.append('0');
		}
		result.append(day);
		result.append('.');

		int month = date.get(Calendar.MONTH) + 1;
		if (month < 10) {
			result.append('0');
		}
		result.append(month);
		result.append('.');

		String year = Integer.toString(date.get(Calendar.YEAR));
		for (int i = year.length(); i < YEAR_MIN_DIGITS; i++) {
			result.append('0');
		}
		result.append(year);

		return result.toString();
	}

	/**
	 * Gets the number of milliseconds that already passed on the current day of the
	 * {@link Calendar}.
	 * 
	 * @param cal The calendar object to get the time from.
	 * @return The time of day in milliseconds.
	 * @throws NullPointerException If {@code cal} is {@code null}.
	 */
	public static int getMsOfDay(Calendar cal) throws NullPointerException {
		Objects.requireNonNull(cal, "The calendar to get time from can't be null.");

		int time = cal.get(Calendar.HOUR_OF_DAY) * 3600000;
		time += cal.get(Calendar.MINUTE) * 60000;
		time += cal.get(Calendar.SECOND) * 1000;
		time += cal.get(Calendar.MILLISECOND);

		return time;
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
		return isNextDay(parseDate(first), parseDate(second));
	}

	/**
	 * Checks whether the time represented by the second {@link Calendar} is on the
	 * day after the time represented by {@code first}.
	 * 
	 * @param first  A {@link Calendar} representing the first time to compare.
	 * @param second A {@link Calendar} with the second time to compare.
	 * @return {@code true} if the day of {@code second} is the next day after
	 *         {@code first}.
	 * @throws NullPointerException If one of the arguments is {@code null}.
	 */
	public static boolean isNextDay(Calendar first, Calendar second) throws NullPointerException {
		Objects.requireNonNull(first, "The first day to check can't be null.");
		Objects.requireNonNull(second, "The second day to check can't be null.");

		Calendar c1 = new GregorianCalendar(first.get(Calendar.YEAR), first.get(Calendar.MONTH),
				first.get(Calendar.DATE));
		c1.add(Calendar.DATE, 1);

		return c1.get(Calendar.YEAR) == second.get(Calendar.YEAR)
				&& c1.get(Calendar.MONTH) == second.get(Calendar.MONTH)
				&& c1.get(Calendar.DATE) == second.get(Calendar.DATE);
	}

	/**
	 * Checks whether the two {@link Calendar} objects represent times on the same
	 * day.
	 * 
	 * @param first  A {@link Calendar} to compare to {@code second}.
	 * @param second A {@link Calendar} to compare to {@code first}.
	 * @return {@code true} if the two {@link Calendar Calendars} are on the same
	 *         day.
	 * @throws NullPointerException If one of the {@link Calendar Calendars} is
	 *                              {@code null}.
	 */
	public static boolean isSameDay(Calendar first, Calendar second) throws NullPointerException {
		Objects.requireNonNull(first, "The first day to check can't be null.");
		Objects.requireNonNull(second, "The second day to check can't be null.");

		return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
				&& first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
				&& first.get(Calendar.DATE) == second.get(Calendar.DATE);
	}

	/**
	 * Sets the character used by {@link #encodeTime} to separate the integer part
	 * and the fractional part of seconds.
	 * 
	 * @param separator The new decimal separator to use.
	 * @throws IllegalArgumentException If {@code separator} is neither a dot nor a
	 *                                  comma.
	 * @see #getDecimalSeparator()
	 */
	public static synchronized void setDecimalSeparator(char separator) throws IllegalArgumentException {
		if (separator != '.' && separator != ',') {
			throw new IllegalArgumentException("Can only use dot or comma as decimal separator.");
		}
		decimal_separator = separator;
	}

	/**
	 * Gets the character used by {@link #encodeTime} to separate the integer part
	 * and the fractional part of the seconds.
	 * 
	 * @return The decimal separator used by this class.
	 * @see #setDecimalSeparator(char)
	 */
	public static synchronized char getDecimalSeparator() {
		return decimal_separator;
	}
}
