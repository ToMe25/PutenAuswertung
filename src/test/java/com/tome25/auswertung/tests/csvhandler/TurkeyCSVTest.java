package com.tome25.auswertung.tests.csvhandler;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.ZoneInfo;
import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.testdata.TurkeyGenerator;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * The class containing unit tests related to the creation of output csv lines.
 * 
 * @author theodor
 */
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
	 * Test converting the first day of a {@link TurkeyInfo} with info about two
	 * days to a csv line.
	 */
	@Test
	public void convertFirstDay() {
		String date = "05.12.2021";
		TurkeyInfo info = getTwoDayInfo(date);
		assertEquals("Getting zone info for the first day of a TurkeyInfo returned an invalid string.",
				"0;05.12.2021;5;11:39:55.28;01:28:33.19;10:51:31.53", CSVHandler.turkeyToCsvLine(info, date, null));
	}

	/**
	 * Test converting the second day of a {@link TurkeyInfo} with info about two
	 * days to a csv line.
	 */
	@Test
	public void convertSecondDay() {
		TurkeyInfo info = getTwoDayInfo("21.09.2023");
		assertEquals("Getting zone info for the first day of a TurkeyInfo returned an invalid string.",
				"0;22.09.2023;5;08:37:29.52;02:50:52.58;12:12:37.92;00:18:59.98",
				CSVHandler.turkeyToCsvLine(info, "22.09.2023", null));
	}

	/**
	 * Test generating a totals line for a two day {@link TurkeyInfo}.
	 */
	@Test
	public void convertMultiDayTotals() {
		TurkeyInfo info = getTwoDayInfo("21.09.2023");
		assertEquals("Getting zone info for the first day of a TurkeyInfo returned an invalid string.",
				"0;total;10;20:17:24.80;04:19:25.77;12:12:37.92;11:10:31.51",
				CSVHandler.turkeyToCsvLine(info, null, null));
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

		Arguments args = Arguments.empty();
		args.fillDays = true;
		List<ZoneInfo> zones = new ArrayList<ZoneInfo>();
		zones.add(new ZoneInfo("Z1", true, "Antenna Z1"));
		zones.add(new ZoneInfo("Zone 2", true, "Antenna 2", "Antenna 3"));
		zones.add(new ZoneInfo("#3", true, "Antenna #3"));
		TurkeyInfo info = TurkeyGenerator.generateTurkey("0", 5, args, null, zones.get(0),
				TimeUtils.parseTime(day, 10510));
		info.changeZone(zones.get(1), TimeUtils.parseTime(day, 20410));
		info.changeZone(zones.get(1), TimeUtils.parseTime(day, 100060));
		info.changeZone(zones.get(0), TimeUtils.parseTime(day, 599610));
		info.changeZone(zones.get(2), TimeUtils.parseTime(day, 1004720));
		info.changeZone(zones.get(1), TimeUtils.parseTime(day, 43000000));
		info.changeZone(zones.get(0), TimeUtils.parseTime(day, 81512330));
		info.endDay(day);

		return info;
	}

	/**
	 * Generates a basic turkey containing zone info for two days.<br/>
	 * The generated {@link TurkeyInfo} has the transponders "T1", "Trans 2", and
	 * "T3".<br/>
	 * It has entered the zones "Z1", "Zone 2", "#3", and "Z4".
	 * 
	 * @param firstDate The first of the two days for which to generate zone info.
	 * @return The generated {@link TurkeyInfo}.
	 * @throws NullPointerException If {@code firstDate} is {@code null}.
	 */
	public static TurkeyInfo getTwoDayInfo(String firstDate) throws NullPointerException {
		Objects.requireNonNull(firstDate, "The first date to use was null.");

		TurkeyInfo ti = getBasicInfo(firstDate);
		Calendar cal = TimeUtils.parseDate(firstDate);
		cal.add(Calendar.DATE, 1);
		List<ZoneInfo> zones = new ArrayList<ZoneInfo>();
		zones.add(new ZoneInfo("Z1", true, "Antenna Z1"));
		zones.add(new ZoneInfo("Zone 2", true, "Antenna 2", "Antenna 3"));
		zones.add(new ZoneInfo("#3", true, "Antenna #3"));
		zones.add(new ZoneInfo("Z4", true, "Antenna Z4"));
		String secondDate = TimeUtils.encodeDate(cal);
		ti.changeZone(zones.get(3), TimeUtils.parseTime(secondDate, 87234));
		ti.changeZone(zones.get(1), TimeUtils.parseTime(secondDate, 2345054));
		ti.changeZone(zones.get(2), TimeUtils.parseTime(secondDate, 3485034));
		ti.changeZone(zones.get(3), TimeUtils.parseTime(secondDate, 34534555));
		ti.changeZone(zones.get(0), TimeUtils.parseTime(secondDate, 76234657));
		ti.endDay(secondDate);

		return ti;
	}

}
