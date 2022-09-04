package com.tome25.auswertung.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

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
	 * Makes sure that encoding a negative time throws a
	 * {@link IllegalArgumentException}.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void negativeMsToString() throws IllegalArgumentException {
		TimeUtils.encodeTime(-12);
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

	/**
	 * Tests parsing a simple date string to a {@link Calendar} object.
	 */
	@Test
	public void dateStringToCal() {
		Calendar cal = TimeUtils.parseDate("12.06.2022");
		Calendar refCal = new GregorianCalendar(2022, Calendar.JUNE, 12, 0, 0, 0);
		refCal.set(Calendar.MILLISECOND, 0);
		assertEquals("The parsed date did not match.", refCal, cal);
	}

	/**
	 * Makes sure that a date with single digit components is handled correctly.
	 */
	@Test
	public void singleDigitStringToCal() {
		Calendar cal = TimeUtils.parseDate("5.3.9");
		Calendar refCal = new GregorianCalendar(9, Calendar.MARCH, 5, 0, 0, 0);
		refCal.set(Calendar.MILLISECOND, 0);
		assertEquals("Parsing a single digit date didn't match.", refCal, cal);
	}

	/**
	 * Tests encoding time from a {@link Calendar} object as a string.
	 */
	@Test
	public void calToString() {
		Calendar cal = Calendar.getInstance();
		cal.set(2023, Calendar.AUGUST, 29);
		assertEquals("The encoded date string representing a calendar object didn't match.", "29.08.2023",
				TimeUtils.encodeDate(cal));
	}

	/**
	 * Makes sure that parsing a date without a year fails.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void noYearStringToCal() throws IllegalArgumentException {
		TimeUtils.parseDate("12.11");
	}

	/**
	 * Makes sure that parsing a null date throws a {@link NullPointerException}.
	 */
	@Test(expected = NullPointerException.class)
	public void nullStringToCal() throws NullPointerException {
		TimeUtils.parseDate(null);
	}

	/**
	 * Makes sure that encoding a null date throws a {@link NullPointerException}.
	 */
	@Test(expected = NullPointerException.class)
	public void nullCalToString() throws NullPointerException {
		TimeUtils.encodeDate(null);
	}

	/**
	 * Makes sure that {@link TimeUtils#isNextDay} correctly detects next days,
	 * including across months.
	 */
	@Test
	public void nextDate() {
		assertTrue("The 16th isn't the next day after the 15th.", TimeUtils.isNextDay("15.12.2022", "16.12.2022"));
		assertTrue("The 1. mar isn't the next day after the 28. feb.", TimeUtils.isNextDay("28.02.2021", "01.03.2021"));
	}

	/**
	 * Makes sure that {@link TimeUtils#isNextDay} fails when it should.
	 */
	@Test
	public void notNextDate() {
		assertFalse("The 20th was detected as the next day after the 10th.",
				TimeUtils.isNextDay("10.02.2022", "20.02.2022"));
		assertFalse("The 15th nov was detected as the next day after the 14th jul.",
				TimeUtils.isNextDay("14.07.2022", "15.11.2022"));
	}

	/**
	 * Makes sure that {@link TimeUtils#isNextDay(Calendar, Calendar)} works on
	 * {@link Calendar} objects that aren't exactly 24 hours apart.
	 */
	@Test
	public void calNextDay() {
		Calendar c1 = Calendar.getInstance();
		c1.add(Calendar.DATE, 1);
		Calendar c2 = Calendar.getInstance();
		c2.roll(Calendar.HOUR, 1);
		assertTrue("TimeUtils isNextDay failed.", TimeUtils.isNextDay(c2, c1));
	}

	/**
	 * Test {@link TimeUtils#isNextDay(Calendar, Calendar)} with two new
	 * {@link Calendar} instances representing the current time.
	 */
	@Test
	public void calNotNextDay() {
		assertFalse("isNextDay returned true on two current calendars.",
				TimeUtils.isNextDay(Calendar.getInstance(), Calendar.getInstance()));
	}

	/**
	 * Confirm functionality of {@link TimeUtils#isSameDay(Calendar, Calendar)} for
	 * {@link Calendar Calendars} on the same day.
	 */
	@Test
	public void calSameDay() {
		Calendar c1 = Calendar.getInstance();
		c1.roll(Calendar.HOUR, 5);
		assertTrue("isSameDay returned false on two same day calendars.",
				TimeUtils.isSameDay(c1, Calendar.getInstance()));
	}

	/**
	 * Test {@link TimeUtils#isSameDay(Calendar, Calendar)} with two new
	 * {@link Calendar} instances representing different days.
	 */
	@Test
	public void calNotSameDay() {
		Calendar c1 = Calendar.getInstance();
		c1.add(Calendar.DATE, 1);
		Calendar c2 = Calendar.getInstance();
		c2.roll(Calendar.HOUR, 1);
		assertFalse("TimeUtils isNextDay failed.", TimeUtils.isSameDay(c2, c1));
	}

	/**
	 * Tests parsing a time of day and date to a single {@link Calendar} object.
	 */
	@Test
	public void parseTimeAndDate() {
		Calendar cal = TimeUtils.parseTime("03.10.2020", "12:54:03.68");
		Calendar refCal = new GregorianCalendar(2020, Calendar.OCTOBER, 3, 12, 54, 3);
		refCal.set(Calendar.MILLISECOND, 680);
		assertEquals("Parsed time with date didn't match.", refCal, cal);
	}

	/**
	 * Tests getting the ms of day of a {@link Calendar}.
	 */
	@Test
	public void calToMs() {
		Calendar cal = new GregorianCalendar();
		cal.set(2022, 5, 21, 13, 49, 23);
		cal.set(Calendar.MILLISECOND, 120);
		assertEquals("The time of day from the calendar didn't match.", 49763120, TimeUtils.getMsOfDay(cal));
	}

}
