package com.tome25.auswertung.tests;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;

import com.tome25.auswertung.AntennaRecord;

/**
 * Some tests for the {@link AntennaRecord} class, currently just the
 * constructor.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class AntennaRecordTest {

	/**
	 * A test creating a new {@link AntennaRecord} using valid arguments.
	 */
	@Test
	public void constructor() {
		AntennaRecord record = new AntennaRecord("Transponder", "02.11.2045", "15:04:22.87", "Antenna 1");
		assertEquals("The transponder id didn't match.", "Transponder", record.transponder);
		assertEquals("The date string didn't match.", "02.11.2045", record.date);
		assertEquals("The antenna id didn't match.", "Antenna 1", record.antenna);
		assertEquals("The time of day didn't match.", 54262870, record.tod);
		GregorianCalendar cal = new GregorianCalendar(2045, Calendar.NOVEMBER, 2, 15, 04, 22);
		cal.add(Calendar.MILLISECOND, 870);
		assertEquals("The calendar didn't match.", cal, record.cal);
	}

	/**
	 * A test creating an {@link AntennaRecord} using a {@code null} Transponder id.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void nullTransponder() throws NullPointerException {
		new AntennaRecord(null, "15.01.2022", "17:00:03.11", "Antenna");
	}

	/**
	 * A test creating an {@link AntennaRecord} with a {@code null} Date.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void nullDate() throws NullPointerException {
		new AntennaRecord("Transponder", null, "05:13:37.05", "A1");
	}

	/**
	 * A test creating an {@link AntennaRecord} using a {@code null} Time.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void nullTime() throws NullPointerException {
		new AntennaRecord("Trans #1", "07.07.2007", null, "Ant #2");
	}

	/**
	 * A test creating an {@link AntennaRecord} with a {@code null} Antenna id.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void nullAntenna() throws NullPointerException {
		new AntennaRecord("T1", "12.02.2002", "13:55:00.92", null);
	}

	/**
	 * A test creating an {@link AntennaRecord} with an empty transponder id.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void emptyTransponder() throws IllegalArgumentException {
		new AntennaRecord("", "11.11.2011", "11:11:11.11", "Ant #1");
	}

	/**
	 * A test creating an {@link AntennaRecord} with an empty date.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void emptyDate() throws IllegalArgumentException {
		new AntennaRecord("Trans #1", "", "07:15:31.63", "A5");
	}

	/**
	 * A test creating an {@link AntennaRecord} with an empty time string.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void emptyTime() throws IllegalArgumentException {
		new AntennaRecord("Trans #2", "12.04.2020", "", "Ant #1");
	}

	/**
	 * A test creating an {@link AntennaRecord} with an empty antenna id.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void emptyAntenna() throws IllegalArgumentException {
		new AntennaRecord("Trans #1", "30.01.2022", "12:45:03.00", "");
	}

	/**
	 * A test creating an {@link AntennaRecord} with a date string with leading
	 * zeroes trimmed.
	 */
	@Test
	public void shortDateComponents() {
		AntennaRecord record = new AntennaRecord("Trans #2", "1.5.21", "00:05:43.00", "Ant #3");
		assertEquals("The transponder id didn't match.", "Trans #2", record.transponder);
		assertEquals("The date string didn't match.", "01.05.0021", record.date);
		assertEquals("The antenna id didn't match.", "Ant #3", record.antenna);
		assertEquals("The time of day didn't match.", 343000, record.tod);
		GregorianCalendar cal = new GregorianCalendar(21, Calendar.MAY, 1, 0, 05, 43);
		assertEquals("The calendar didn't match.", cal, record.cal);
	}

	/**
	 * A test creating an {@link AntennaRecord} with an invalid date.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void invalidDate() throws IllegalArgumentException {
		new AntennaRecord("Trans", "31.04.2022", "06:05:04.03", "Ant");
	}

	/**
	 * A test creating an {@link AntennaRecord} with an invalid time string.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void invalidTime() throws IllegalArgumentException {
		new AntennaRecord("Transponder 1", "28.02.2021", "24:66:", "Antenna");
	}

	/**
	 * A test creating an {@link AntennaRecord} with a time string representing an
	 * illegal time of day.
	 * 
	 * @throws IllegalArgumentException Always.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void invalidTimeOfDay() throws IllegalArgumentException {
		new AntennaRecord("Transponder 1", "28.02.2021", "24:66:00.00", "Antenna");
	}

}
