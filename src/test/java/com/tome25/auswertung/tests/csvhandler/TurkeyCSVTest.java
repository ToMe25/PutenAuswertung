package com.tome25.auswertung.tests.csvhandler;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Objects;

import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * The class containing unit tests related to the creation of output csv lines.
 * 
 * @author theodor
 */
//TODO Add multi day tests
public class TurkeyCSVTest {

	/**
	 * A simple unit test verifying that the turkey csv header line is generated
	 * correctly.
	 */
	@Test
	public void turkeyHeader() {
		assertEquals("The turkey csv header did not match.",
				"Tier;Datum;Bereichswechsel;Aufenthalt in Zone Zone 1;Aufenthalt in Zone Zone 2",
				CSVHandler.turkeyCsvHeader(Arrays.asList(new String[] { "Zone 1", "Zone 2" })));

		assertEquals("The turkey csv header did not match.",
				"Tier;Datum;Bereichswechsel;Aufenthalt in Zone Z1;Aufenthalt in Zone Zone 2;Aufenthalt in Zone #3",
				CSVHandler.turkeyCsvHeader(Arrays.asList(new String[] { "Z1", "Zone 2", "#3" })));
	}

	/**
	 * Tests the most basic use case of converting a {@link TurkeyInfo} to a csv
	 * line.
	 */
	@Test
	public void convertBasic() {
		String date = "01.01.2022";
		TurkeyInfo info = getBasicInfo(date);
		assertEquals("Getting single day info from a basic TurkeyInfo returned an invalid string.",
				"0;01.01.2022;5;01:28:33.19;10:51:31.53;11:39:55.28",
				CSVHandler.turkeyToCsvLine(info, date, Arrays.asList(new String[] { "Z1", "Zone 2", "#3" })));
	}

	/**
	 * Makes sure that converting a {@code null} {@link TurkeyInfo} to a csv line
	 * throws an {@link NullPointerException}.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void convertNullTurkey() throws NullPointerException {
		CSVHandler.turkeyToCsvLine(null, "01.01.2022", Arrays.asList(new String[] { "Z1", "Zone 2", "#3" }));
	}

	/**
	 * Generates a total csv line for a single day {@link TurkeyInfo}.
	 */
	@Test
	public void convertBasicNullDate() {
		String date = "01.01.2022";
		TurkeyInfo info = getBasicInfo(date);
		assertEquals("Getting single day info from a basic TurkeyInfo returned an invalid string.",
				"0;total;5;01:28:33.19;10:51:31.53;11:39:55.28",
				CSVHandler.turkeyToCsvLine(info, null, Arrays.asList(new String[] { "Z1", "Zone 2", "#3" })));
	}

	/**
	 * Generates a csv line without an explicit list of zones to write.
	 */
	@Test
	public void convertNullZones() {
		String date = "01.01.2022";
		TurkeyInfo info = getBasicInfo(date);
		assertEquals("Getting single day info from a basic TurkeyInfo returned an invalid string.",
				"0;01.01.2022;5;11:39:55.28;01:28:33.19;10:51:31.53", CSVHandler.turkeyToCsvLine(info, date, null));
	}

	/**
	 * Test converting a zone the {@link TurkeyInfo} didn't enter in addition to its
	 * zones.
	 */
	@Test
	public void convertEmptyZones() {
		String date = "01.01.2022";
		TurkeyInfo info = getBasicInfo(date);
		assertEquals("Getting single day info from a basic TurkeyInfo returned an invalid string.",
				"0;01.01.2022;5;01:28:33.19;10:51:31.53;11:39:55.28;00:00:00.00",
				CSVHandler.turkeyToCsvLine(info, date, Arrays.asList(new String[] { "Z1", "Zone 2", "#3", "Zone 4" })));
	}

	/**
	 * Converts only some zones of a {@link TurkeyInfo} to a csv line.
	 */
	@Test
	public void convertIncompleteZones() {
		String date = "01.01.2022";
		TurkeyInfo info = getBasicInfo(date);
		assertEquals("Getting single day info from a basic TurkeyInfo returned an invalid string.",
				"0;01.01.2022;5;01:28:33.19;11:39:55.28",
				CSVHandler.turkeyToCsvLine(info, date, Arrays.asList(new String[] { "Z1", "#3" })));
	}

	/**
	 * Generates a basic {@link TurkeyInfo} for testing.<br/>
	 * The generated {@link TurkeyInfo} has the transponders "T1", "Trans 2", and
	 * "T3".<br/>
	 * It only contains info about the given day.<br/>
	 * It has entered the zones "Z1", "Zone 2", and "#3".<br/>
	 * Since actual input can only use hundredths this too doesn't use millisecond
	 * times.
	 * 
	 * @param day The day for which to generate the info.
	 * @return The generated turkey info.
	 * @throws NullPointerException if {@code day} is {@code null}.
	 */
	public static TurkeyInfo getBasicInfo(String day) throws NullPointerException {
		Objects.requireNonNull(day, "The day to generate for can't be null.");

		TurkeyInfo info = new TurkeyInfo("0", Arrays.asList(new String[] { "T1", "Trans 2", "T3" }), null, "Z1",
				TimeUtils.parseTime(day, 10510), null);
		info.changeZone("Zone 2", TimeUtils.parseTime(day, 20410));
		info.changeZone("Zone 2", TimeUtils.parseTime(day, 100060));
		info.changeZone("Z1", TimeUtils.parseTime(day, 599610));
		info.changeZone("#3", TimeUtils.parseTime(day, 1004720));
		info.changeZone("Zone 2", TimeUtils.parseTime(day, 43000000));
		info.changeZone("Z1", TimeUtils.parseTime(day, 81512330));
		info.endDay(day);

		return info;
	}

}
