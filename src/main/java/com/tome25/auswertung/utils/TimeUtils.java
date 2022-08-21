package com.tome25.auswertung.utils;

/**
 * A class containing utility methods related to timestamp, time, and date
 * handling.
 * 
 * @author theodor
 */
public class TimeUtils {

	/**
	 * Converts the given time in the format "HH:MM:SS.2" to time of day in
	 * milliseconds.
	 * 
	 * @param time The time to convert.
	 * @return The parsed time.
	 * @throws NumberFormatException if part of the time string is not contain a
	 *                               parsable integer.
	 */
	public static int parseTime(String time) throws NumberFormatException {
		int result = 0;
		String split[] = time.split(":");

		result += Integer.parseInt(split[0]) * 3600000; // milliseconds per hour
		result += Integer.parseInt(split[1]) * 60000; // milliseconds per minute

		String seconds_split[] = split[2].split("\\.");
		result += Integer.parseInt(seconds_split[0]) * 1000;
		result += Integer.parseInt(seconds_split[1]) * 10;

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
