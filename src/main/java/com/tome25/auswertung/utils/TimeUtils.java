package com.tome25.auswertung.utils;

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
	 * @throws NumberFormatException    if part of the time string is not contain a
	 *                                  parsable integer.
	 * @throws NullPointerException     If {@code time} is {@code null}.
	 * @throws IllegalArgumentException If {@code time} does not match the required
	 *                                  input format("HH:MM:SS.2").
	 */
	public static long parseTime(String time)
			throws NumberFormatException, NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(time, "The time to parse cannot be null.");

		long result = 0;
		String split[] = time.split(":");

		if (split.length != 3) {
			throw new IllegalArgumentException("Time string \"" + time + "\" does not match format.");
		}

		result += Integer.parseInt(split[0]) * 3600000l; // milliseconds per hour
		result += Integer.parseInt(split[1]) * 60000l; // milliseconds per minute

		String seconds_split[] = split[2].split("\\.");

		if (seconds_split.length > 2) {
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
	 */
	public static String encodeTime(long time) {
		int hours = (int) (time / 3600000); // 60 minutes * 60 seconds * 1000 ms
		int minutes = (int) (time % 3600000 / 60000);
		int seconds = (int) (time % 60000 / 1000);
		int hundredths = (int) (time % 1000 / 10);

		return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths);
	}
}
