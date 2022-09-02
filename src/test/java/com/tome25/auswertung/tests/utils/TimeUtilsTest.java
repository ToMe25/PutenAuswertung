package com.tome25.auswertung.tests.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.tome25.auswertung.utils.TimeUtils;

/**
 * The class containing the unit tests related to {@link TimeUtils}.
 * 
 * @author theodor
 */
public class TimeUtilsTest {

	/**
	 * Tests converting a basic time in milliseconds to a string.<br/>
	 * Does not test a time with more than two digits of hours.
	 */
	@Test
	public void msToString() {
		assertEquals("Converting a ms number to a string did not return the expected result.", "05:17:53.51",
				TimeUtils.encodeTime(19073510));
		assertEquals("The string version of a longer time did not match.", "21:11:47.93",
				TimeUtils.encodeTime(76307930));
	}

	/**
	 * Tests encoding a time of more than 100 hours, that is also more than
	 * {@link java.lang.Integer#MAX_VALUE INT_MAX}.
	 */
	@Test
	public void longMsToString() {
		assertEquals("Encoding a long time as a string did not return the correct result.", "3150:34:15.93",
				TimeUtils.encodeTime(11342055930l));
	}

	/**
	 * Tests encoding a time with zero hours to make sure it doesn't skip hours
	 * entirely.
	 */
	@Test
	public void noHoursMsToString() {
		assertEquals("Encoding a time without hours didn't work.", "00:05:09.03", TimeUtils.encodeTime(309030));
	}

	/**
	 * Convert a basic time string to its millisecond representation.
	 */
	@Test
	public void basicStringToMs() {
		assertEquals("Parsing a basic time string didn't return the expected result.", 56851950,
				TimeUtils.parseTime("15:47:31.95"));
	}

	/**
	 * Test converting a long time from string to ms.
	 */
	@Test
	public void longStringToMs() {
		assertEquals("Parsing the string representation of a long time didn't return expected result.", 187154372290l,
				TimeUtils.parseTime("51987:19:32.29"));
	}

	/**
	 * Tests parsing a time made from single digit time components.<br/>
	 * Both with and without zero padding.
	 */
	@Test
	public void singleDigitsStringToMs() {
		assertEquals("Parsing a time with single digit components without padding didn't match.", 3723400,
				TimeUtils.parseTime("1:2:3.4"));// 3.4 seconds are 3 seconds and 400 ms
		assertEquals("Parsing a time with single digit components with padding didn't match.", 3723400,
				TimeUtils.parseTime("01:02:03.40"));
	}

	/**
	 * Tests parsing a time of less than an hour.
	 */
	@Test
	public void zeroHoursStringToMs() {
		assertEquals("Parsing a time with 0 hours didn't match.", 943730, TimeUtils.parseTime("0:15:43.73"));
	}

	/**
	 * Tests parsing a string time that does not have hundredths.
	 */
	@Test
	public void noHundredthsStringToMs() {
		assertEquals("Parsing a time without hundredths didn't work.", 50301000, TimeUtils.parseTime("13:58:21"));
	}

	/**
	 * Makes sure that parsing a {@code null} time string throws a
	 * {@code NullPointerException}.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void nullStringToMs() throws NullPointerException {
		TimeUtils.parseTime(null);
	}

	/**
	 * Ensures that parsing a time without hours fails.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void noHoursStringToMs() throws IllegalArgumentException {
		TimeUtils.parseTime("21:35");
	}

	/**
	 * Makes sure that parsing a time with millisecond precision fails.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void msStringToMs() throws IllegalArgumentException {
		TimeUtils.parseTime("12:41:21.132");
	}

}
