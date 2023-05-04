package com.tome25.auswertung.tests;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static com.tome25.auswertung.utils.TimeUtils.DAY_MS;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.DataHandler;
import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.ZoneInfo;
import com.tome25.auswertung.ZoneStay;
import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.testdata.AntennaDataGenerator;
import com.tome25.auswertung.testdata.AntennaDataGenerator.TestData;
import com.tome25.auswertung.testdata.TurkeyGenerator;
import com.tome25.auswertung.testdata.ZoneGenerator;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * A class containing tests to verify the correctness of the generated output
 * data.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class OutputDataTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	/**
	 * A manually written antenna records file for testing zones times of less than
	 * 5 mins crossing the day border.
	 * 
	 * @throws IOException If reading/writing/creating a temp file fails.
	 */
	@Test
	public void shortCrossDate() throws IOException {
		final TestMappings mappings = generateTestMappings(2, 3, 5, false, 0, 0, tempFolder);
		List<TurkeyInfo> turkeys = mappings.turkeys;
		List<ZoneInfo> zones = mappings.zones;
		FileInputStreamHandler turkeysIn = mappings.turkeysIn;
		FileInputStreamHandler zonesIn = mappings.zonesIn;
		String t1 = turkeys.get(0).getTransponders().get(0);
		String t2 = turkeys.get(1).getTransponders().get(0);
		String a1 = zones.get(0).getAntennas().get(0);
		String a2 = zones.get(1).getAntennas().get(0);
		String a3 = zones.get(2).getAntennas().get(0);

		Pair<FileInputStreamHandler, PrintStream> antennaPair = tempFolder.newTempInputFile("antenna.csv");
		FileInputStreamHandler antennaIn = antennaPair.getKey();
		PrintStream aps = antennaPair.getValue();
		aps.printf("%s;06.03.2022;02:12:45.32;%s%n", t1, a2);
		aps.printf("%s;06.03.2022;02:18:17.96;%s%n", t1, a3);
		aps.printf("%s;06.03.2022;03:03:12.05;%s%n", t2, a1);
		aps.printf("%s;06.03.2022;03:12:31.85;%s%n", t1, a3);
		aps.printf("%s;06.03.2022;03:27:01.44;%s%n", t2, a2);
		aps.printf("%s;06.03.2022;03:31:26.48;%s%n", t2, a1);
		aps.printf("%s;06.03.2022;05:14:51.19;%s%n", t2, a1);
		aps.printf("%s;06.03.2022;06:46:50.64;%s%n", t2, a2);
		aps.printf("%s;06.03.2022;08:52:26.73;%s%n", t1, a2);
		aps.printf("%s;06.03.2022;08:55:45.52;%s%n", t1, a3);
		aps.printf("%s;06.03.2022;08:58:23.48;%s%n", t1, a1);
		aps.printf("%s;06.03.2022;15:26:08.53;%s%n", t2, a1);
		aps.printf("%s;06.03.2022;18:43:52.67;%s%n", t1, a2);
		aps.printf("%s;06.03.2022;20:38:12.29;%s%n", t2, a1);
		aps.printf("%s;06.03.2022;23:52:45.89;%s%n", t1, a3);
		aps.printf("%s;06.03.2022;23:58:12.76;%s%n", t1, a2);
		aps.printf("%s;06.03.2022;23:58:31.32;%s%n", t2, a2);
		aps.printf("%s;07.03.2022;00:02:26.92;%s%n", t2, a3);
		aps.printf("%s;07.03.2022;00:02:34.57;%s%n", t1, a3);
		aps.printf("%s;07.03.2022;00:06:51.28;%s%n", t2, a3);
		aps.printf("%s;07.03.2022;00:07:41.81;%s%n", t1, a1);
		aps.printf("%s;07.03.2022;00:09:28.09;%s%n", t1, a2);
		aps.printf("%s;07.03.2022;00:13:29.15;%s%n", t2, a2);
		aps.printf("%s;07.03.2022;01:10:37.73;%s%n", t1, a3);
		aps.printf("%s;07.03.2022;01:19:51.21;%s%n", t2, a1);
		aps.printf("%s;07.03.2022;01:21:42.86;%s%n", t1, a2);
		aps.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsPair = tempFolder.newTempIOFile("totals.csv");
		FileOutputStreamHandler totalsOut = totalsPair.getValue();
		FileInputStreamHandler totalsIn = totalsPair.getKey();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> staysPair = tempFolder.newTempIOFile("stays.csv");
		FileOutputStreamHandler staysOut = staysPair.getValue();
		FileInputStreamHandler staysIn = staysPair.getKey();
		DataHandler.handleStreams(antennaIn, turkeysIn, zonesIn, null, totalsOut, staysOut, Arguments.empty());
		totalsOut.close();
		staysOut.close();

		Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> totalsData = CSVHandler
				.readTotalsCSV(totalsIn);
		totalsIn.close();

		Map<String, Map<String, Map<String, Long>>> outputTimes = totalsData.getKey();
		Map<String, Map<String, Integer>> outputChanges = totalsData.getValue();

		assertEquals("First day turkey \"0\" changes count didn't match.", 4,
				(int) outputChanges.get("0").get("06.03.2022"));
		assertEquals("Second day turkey \"0\" changes count didn't match.", 3,
				(int) outputChanges.get("0").get("07.03.2022"));
		assertEquals("Total turkey \"0\" changes count didn't match.", 7, (int) outputChanges.get("0").get("total"));

		assertEquals("First day turkey \"1\" changes count didn't match.", 2,
				(int) outputChanges.get("1").get("06.03.2022"));
		assertEquals("Second day turkey \"1\" changes count didn't match.", 3,
				(int) outputChanges.get("1").get("07.03.2022"));
		assertEquals("Total turkey \"1\" changes count didn't match.", 5, (int) outputChanges.get("1").get("total"));

		assertEquals("First day turkey \"0\" Zone 1 time didn't match.", TimeUtils.parseTime("09:45:29.19"),
				(long) outputTimes.get("0").get("06.03.2022").get("Zone 1"));
		assertEquals("First day turkey \"0\" Zone 2 time didn't match.", TimeUtils.parseTime("05:14:25.86"),
				(long) outputTimes.get("0").get("06.03.2022").get("Zone 2"));
		assertEquals("First day turkey \"0\" Zone 3 time didn't match.", TimeUtils.parseTime("06:47:19.63"),
				(long) outputTimes.get("0").get("06.03.2022").get("Zone 3"));

		assertEquals("First day turkey \"1\" Zone 1 time didn't match.", TimeUtils.parseTime("13:07:56.79"),
				(long) outputTimes.get("1").get("06.03.2022").get("Zone 1"));
		assertEquals("First day turkey \"1\" Zone 2 time didn't match.", TimeUtils.parseTime("08:39:17.89"),
				(long) outputTimes.get("1").get("06.03.2022").get("Zone 2"));
		assertEquals("First day turkey \"1\" Zone 3 time didn't match.", 0,
				(long) outputTimes.get("1").get("06.03.2022").get("Zone 3"));

		assertEquals("Second day turkey \"0\" Zone 1 time didn't match.", 0,
				(long) outputTimes.get("0").get("07.03.2022").get("Zone 1"));
		assertEquals("Second day turkey \"0\" Zone 2 time didn't match.", TimeUtils.parseTime("01:01:09.64"),
				(long) outputTimes.get("0").get("07.03.2022").get("Zone 2"));
		assertEquals("Second day turkey \"0\" Zone 3 time didn't match.", TimeUtils.parseTime("00:20:33.22"),
				(long) outputTimes.get("0").get("07.03.2022").get("Zone 3"));

		assertEquals("Second day turkey \"1\" Zone 1 time didn't match.", TimeUtils.parseTime("00:04:18.57"),
				(long) outputTimes.get("1").get("07.03.2022").get("Zone 1"));
		assertEquals("Second day turkey \"1\" Zone 2 time didn't match.", TimeUtils.parseTime("01:06:22.06"),
				(long) outputTimes.get("1").get("07.03.2022").get("Zone 2"));
		assertEquals("Second day turkey \"1\" Zone 3 time didn't match.", TimeUtils.parseTime("00:11:02.23"),
				(long) outputTimes.get("1").get("07.03.2022").get("Zone 3"));

		assertEquals("Total turkey \"0\" Zone 1 time didn't match.", TimeUtils.parseTime("09:45:29.19"),
				(long) outputTimes.get("0").get("total").get("Zone 1"));
		assertEquals("Total turkey \"0\" Zone 2 time didn't match.", TimeUtils.parseTime("06:15:35.50"),
				(long) outputTimes.get("0").get("total").get("Zone 2"));
		assertEquals("Total turkey \"0\" Zone 3 time didn't match.", TimeUtils.parseTime("07:07:52.85"),
				(long) outputTimes.get("0").get("total").get("Zone 3"));

		assertEquals("Total turkey \"1\" Zone 1 time didn't match.", TimeUtils.parseTime("13:12:15.36"),
				(long) outputTimes.get("1").get("total").get("Zone 1"));
		assertEquals("Total turkey \"1\" Zone 2 time didn't match.", TimeUtils.parseTime("09:45:39.95"),
				(long) outputTimes.get("1").get("total").get("Zone 2"));
		assertEquals("Total turkey \"1\" Zone 3 time didn't match.", TimeUtils.parseTime("00:11:02.23"),
				(long) outputTimes.get("1").get("total").get("Zone 3"));

		Map<String, List<ZoneStay>> staysData = CSVHandler.readStaysCSV(staysIn, mappings.zones);
		staysIn.close();

		List<ZoneStay> t1Stays = new ArrayList<ZoneStay>();
		t1Stays.add(new ZoneStay("0", zones.get(1), TimeUtils.parseTime("06.03.2022", "02:12:45.32"),
				TimeUtils.parseTime("06.03.2022", "02:18:17.96")));
		t1Stays.add(new ZoneStay("0", zones.get(2), TimeUtils.parseTime("06.03.2022", "02:18:17.96"),
				TimeUtils.parseTime("06.03.2022", "08:58:23.48")));
		t1Stays.add(new ZoneStay("0", zones.get(0), TimeUtils.parseTime("06.03.2022", "08:58:23.48"),
				TimeUtils.parseTime("06.03.2022", "18:43:52.67")));
		t1Stays.add(new ZoneStay("0", zones.get(1), TimeUtils.parseTime("06.03.2022", "18:43:52.67"),
				TimeUtils.parseTime("06.03.2022", "23:52:45.89")));
		t1Stays.add(new ZoneStay("0", zones.get(2), TimeUtils.parseTime("06.03.2022", "23:52:45.89"),
				TimeUtils.parseTime("07.03.2022", "00:09:28.09")));
		t1Stays.add(new ZoneStay("0", zones.get(1), TimeUtils.parseTime("07.03.2022", "00:09:28.09"),
				TimeUtils.parseTime("07.03.2022", "01:10:37.73")));
		t1Stays.add(new ZoneStay("0", zones.get(2), TimeUtils.parseTime("07.03.2022", "01:10:37.73"),
				TimeUtils.parseTime("07.03.2022", "01:21:42.86")));

		assertEquals("The number of zone stays for turkey \"0\" didn't match.", t1Stays.size(),
				staysData.get("0").size());

		for (int i = 0; i < staysData.get("0").size() && i < t1Stays.size(); i++) {
			assertEquals("A zone stay for turkey \"0\" didn't match.", t1Stays.get(i), staysData.get("0").get(i));
		}

		List<ZoneStay> t2Stays = new ArrayList<ZoneStay>();
		t2Stays.add(new ZoneStay("1", zones.get(0), TimeUtils.parseTime("06.03.2022", "02:12:45.32"),
				TimeUtils.parseTime("06.03.2022", "06:46:50.64")));
		t2Stays.add(new ZoneStay("1", zones.get(1), TimeUtils.parseTime("06.03.2022", "06:46:50.64"),
				TimeUtils.parseTime("06.03.2022", "15:26:08.53")));
		t2Stays.add(new ZoneStay("1", zones.get(0), TimeUtils.parseTime("06.03.2022", "15:26:08.53"),
				TimeUtils.parseTime("07.03.2022", "00:02:26.92")));
		t2Stays.add(new ZoneStay("1", zones.get(2), TimeUtils.parseTime("07.03.2022", "00:02:26.92"),
				TimeUtils.parseTime("07.03.2022", "00:13:29.15")));
		t2Stays.add(new ZoneStay("1", zones.get(1), TimeUtils.parseTime("07.03.2022", "00:13:29.15"),
				TimeUtils.parseTime("07.03.2022", "01:19:51.21")));
		t2Stays.add(new ZoneStay("1", zones.get(0), TimeUtils.parseTime("07.03.2022", "01:19:51.21"),
				TimeUtils.parseTime("07.03.2022", "01:21:42.86")));

		assertEquals("The number of zone stays for turkey \"1\" didn't match.", t2Stays.size(),
				staysData.get("1").size());

		for (int i = 0; i < staysData.get("1").size() && i < t2Stays.size(); i++) {
			assertEquals("A zone stay for turkey \"1\" didn't match.", t2Stays.get(i), staysData.get("1").get(i));
		}
	}

	/**
	 * A manually written unit test containing a stay with a length of one month.
	 * 
	 * @throws IOException If reading/writing/creating a temp file fails.
	 */
	@Test
	public void oneMonthStay() throws IOException {
		final TestMappings mappings = generateTestMappings(2, 2, 5, false, 0, 0, tempFolder);
		List<TurkeyInfo> turkeys = mappings.turkeys;
		List<ZoneInfo> zones = mappings.zones;
		FileInputStreamHandler turkeysIn = mappings.turkeysIn;
		FileInputStreamHandler zonesIn = mappings.zonesIn;
		String t1 = turkeys.get(0).getTransponders().get(0);
		String t2 = turkeys.get(1).getTransponders().get(0);
		String a1 = zones.get(0).getAntennas().get(0);
		String a2 = zones.get(1).getAntennas().get(0);

		Pair<FileInputStreamHandler, PrintStream> antennaPair = tempFolder.newTempInputFile("antenna.csv");
		FileInputStreamHandler antennaIn = antennaPair.getKey();
		PrintStream aps = antennaPair.getValue();
		aps.printf("%s;06.05.2023;02:05:42.59;%s%n", t1, a2);
		aps.printf("%s;06.05.2023;02:17:19.38;%s%n", t1, a1);
		aps.printf("%s;06.05.2023;03:00:54.07;%s%n", t2, a1);
		aps.printf("%s;06.05.2023;03:59:37.72;%s%n", t1, a1);
		aps.printf("%s;06.05.2023;05:01:08.21;%s%n", t1, a2);
		aps.printf("%s;06.05.2023;11:29:46.41;%s%n", t2, a2);
		aps.printf("%s;07.05.2023;16:53:43.60;%s%n", t1, a2);
		aps.printf("%s;08.05.2023;13:46:11.77;%s%n", t1, a2);
		aps.printf("%s;09.05.2023;09:36:59.97;%s%n", t1, a2);
		aps.printf("%s;10.05.2023;16:48:43.51;%s%n", t1, a2);
		aps.printf("%s;11.05.2023;07:04:23.84;%s%n", t1, a2);
		aps.printf("%s;12.05.2023;15:44:31.99;%s%n", t1, a1);
		aps.printf("%s;13.05.2023;09:51:38.84;%s%n", t1, a1);
		aps.printf("%s;14.05.2023;19:34:29.19;%s%n", t1, a1);
		aps.printf("%s;15.05.2023;22:20:57.74;%s%n", t1, a1);
		aps.printf("%s;16.05.2023;15:38:36.63;%s%n", t1, a1);
		aps.printf("%s;17.05.2023;01:24:08.00;%s%n", t1, a1);
		aps.printf("%s;18.05.2023;23:59:01.00;%s%n", t1, a1);
		aps.printf("%s;19.05.2023;13:37:46.71;%s%n", t1, a1);
		aps.printf("%s;20.05.2023;21:18:53.08;%s%n", t1, a1);
		aps.printf("%s;21.05.2023;02:35:20.83;%s%n", t1, a2);
		aps.printf("%s;22.05.2023;12:09:33.41;%s%n", t1, a2);
		aps.printf("%s;23.05.2023;03:43:18.32;%s%n", t1, a2);
		aps.printf("%s;24.05.2023;20:52:44.79;%s%n", t1, a2);
		aps.printf("%s;25.05.2023;18:16:02.09;%s%n", t1, a2);
		aps.printf("%s;26.05.2023;05:46:57.63;%s%n", t1, a2);
		aps.printf("%s;27.05.2023;02:53:03.76;%s%n", t1, a2);
		aps.printf("%s;28.05.2023;12:06:15.25;%s%n", t1, a2);
		aps.printf("%s;29.05.2023;15:44:18.36;%s%n", t1, a2);
		aps.printf("%s;30.05.2023;22:27:23.31;%s%n", t1, a2);
		aps.printf("%s;31.05.2023;18:00:48.21;%s%n", t1, a1);
		aps.printf("%s;01.06.2023;00:58:23.54;%s%n", t1, a1);
		aps.printf("%s;02.06.2023;16:21:22.61;%s%n", t1, a1);
		aps.printf("%s;03.06.2023;18:03:20.43;%s%n", t1, a1);
		aps.printf("%s;04.06.2023;06:59:23.48;%s%n", t1, a1);
		aps.printf("%s;05.06.2023;16:48:21.16;%s%n", t1, a1);
		aps.printf("%s;06.06.2023;03:08:27.44;%s%n", t1, a2);
		aps.printf("%s;06.06.2023;12:31:00.53;%s%n", t2, a1);
		aps.printf("%s;06.06.2023;13:55:41.00;%s%n", t1, a1);
		aps.printf("%s;06.06.2023;13:55:41.55;%s%n", t2, a2);
		aps.printf("%s;07.06.2023;03:12:51.00;%s%n", t2, a1);
		aps.printf("%s;07.06.2023;05:08:38.63;%s%n", t1, a1);
		aps.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsPair = tempFolder.newTempIOFile("totals.csv");
		FileOutputStreamHandler totalsOut = totalsPair.getValue();
		FileInputStreamHandler totalsIn = totalsPair.getKey();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> staysPair = tempFolder.newTempIOFile("stays.csv");
		FileOutputStreamHandler staysOut = staysPair.getValue();
		FileInputStreamHandler staysIn = staysPair.getKey();
		DataHandler.handleStreams(antennaIn, turkeysIn, zonesIn, null, totalsOut, staysOut, Arguments.empty());
		totalsOut.close();
		staysOut.close();

		Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> totalsData = CSVHandler
				.readTotalsCSV(totalsIn);
		totalsIn.close();

		Map<String, Map<String, Map<String, Long>>> outputTimes = totalsData.getKey();
		Map<String, Map<String, Integer>> outputChanges = totalsData.getValue();

		assertEquals("Zone changes for turkey \"0\" on date 06.05.2023 didn't match.", 2,
				(int) outputChanges.get("0").get("06.05.2023"));
		assertEquals("Zone 1 time for turkey \"0\" on date 06.05.2023 didn't match.",
				TimeUtils.parseTime("02:43:48.83"), (long) outputTimes.get("0").get("06.05.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"0\" on date 06.05.2023 didn't match.",
				TimeUtils.parseTime("19:10:28.58"), (long) outputTimes.get("0").get("06.05.2023").get("Zone 2"));

		assertEquals("Zone changes for turkey \"1\" on date 06.05.2023 didn't match.", 1,
				(int) outputChanges.get("1").get("06.05.2023"));
		assertEquals("Zone 1 time for turkey \"1\" on date 06.05.2023 didn't match.",
				TimeUtils.parseTime("09:24:03.82"), (long) outputTimes.get("1").get("06.05.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"1\" on date 06.05.2023 didn't match.",
				TimeUtils.parseTime("12:30:13.59"), (long) outputTimes.get("1").get("06.05.2023").get("Zone 2"));

		for (int date = 7; date < 12; date++) {
			String dateStr = String.format("%02d.05.2023", date);
			assertEquals("Zone changes for turkey \"0\" on date " + dateStr + " didn't match.", 0,
					(int) outputChanges.get("0").get(dateStr));
			assertEquals("Zone 1 time for turkey \"0\" on date " + dateStr + " didn't match.", 0,
					(long) outputTimes.get("0").get(dateStr).get("Zone 1"));
			assertEquals("Zone 2 time for turkey \"0\" on date " + dateStr + " didn't match.", DAY_MS,
					(long) outputTimes.get("0").get(dateStr).get("Zone 2"));
		}

		assertEquals("Zone changes for turkey \"0\" on date 12.05.2023 didn't match.", 1,
				(int) outputChanges.get("0").get("12.05.2023"));
		assertEquals("Zone 1 time for turkey \"0\" on date 12.05.2023 didn't match.",
				TimeUtils.parseTime("08:15:28.01"), (long) outputTimes.get("0").get("12.05.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"0\" on date 12.05.2023 didn't match.",
				TimeUtils.parseTime("15:44:31.99"), (long) outputTimes.get("0").get("12.05.2023").get("Zone 2"));

		for (int date = 13; date < 21; date++) {
			String dateStr = String.format("%d.05.2023", date);
			assertEquals("Zone changes for turkey \"0\" on date " + dateStr + " didn't match.", 0,
					(int) outputChanges.get("0").get(dateStr));
			assertEquals("Zone 1 time for turkey \"0\" on date " + dateStr + " didn't match.", DAY_MS,
					(long) outputTimes.get("0").get(dateStr).get("Zone 1"));
			assertEquals("Zone 2 time for turkey \"0\" on date " + dateStr + " didn't match.", 0,
					(long) outputTimes.get("0").get(dateStr).get("Zone 2"));
		}

		assertEquals("Zone changes for turkey \"0\" date 21.05.2023 didn't match.", 1,
				(int) outputChanges.get("0").get("21.05.2023"));
		assertEquals("Zone 1 time for turkey \"0\" on date 21.05.2023 didn't match.",
				TimeUtils.parseTime("02:35:20.83"), (long) outputTimes.get("0").get("21.05.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"0\" on date 21.05.2023 didn't match.",
				TimeUtils.parseTime("21:24:39.17"), (long) outputTimes.get("0").get("21.05.2023").get("Zone 2"));

		for (int date = 22; date < 31; date++) {
			String dateStr = String.format("%d.05.2023", date);
			assertEquals("Zone changes for turkey \"0\" on date " + dateStr + " didn't match.", 0,
					(int) outputChanges.get("0").get(dateStr));
			assertEquals("Zone 1 time for turkey \"0\" on date " + dateStr + " didn't match.", 0,
					(long) outputTimes.get("0").get(dateStr).get("Zone 1"));
			assertEquals("Zone 2 time for turkey \"0\" on date " + dateStr + " didn't match.", DAY_MS,
					(long) outputTimes.get("0").get(dateStr).get("Zone 2"));
		}

		assertEquals("Zone changes for turkey \"0\" date 31.05.2023 didn't match.", 1,
				(int) outputChanges.get("0").get("31.05.2023"));
		assertEquals("Zone 1 time for turkey \"0\" on date 31.05.2023 didn't match.",
				TimeUtils.parseTime("05:59:11.79"), (long) outputTimes.get("0").get("31.05.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"0\" on date 31.05.2023 didn't match.",
				TimeUtils.parseTime("18:00:48.21"), (long) outputTimes.get("0").get("31.05.2023").get("Zone 2"));

		for (int date = 7; date <= 31; date++) {
			String dateStr = String.format("%02d.05.2023", date);
			assertEquals("Zone changes for turkey \"1\" on date " + dateStr + " didn't match.", 0,
					(int) outputChanges.get("1").get(dateStr));
			assertEquals("Zone 1 time for turkey \"1\" on date " + dateStr + " didn't match.", 0,
					(long) outputTimes.get("1").get(dateStr).get("Zone 1"));
			assertEquals("Zone 2 time for turkey \"1\" on date " + dateStr + " didn't match.", DAY_MS,
					(long) outputTimes.get("1").get(dateStr).get("Zone 2"));
		}

		for (int date = 1; date < 6; date++) {
			String dateStr = String.format("%02d.06.2023", date);
			assertEquals("Zone changes for turkey \"0\" on date " + dateStr + " didn't match.", 0,
					(int) outputChanges.get("0").get(dateStr));
			assertEquals("Zone 1 time for turkey \"0\" on date " + dateStr + " didn't match.", DAY_MS,
					(long) outputTimes.get("0").get(dateStr).get("Zone 1"));
			assertEquals("Zone 2 time for turkey \"0\" on date " + dateStr + " didn't match.", 0,
					(long) outputTimes.get("0").get(dateStr).get("Zone 2"));
		}

		for (int date = 1; date < 6; date++) {
			String dateStr = String.format("%02d.06.2023", date);
			assertEquals("Zone changes for turkey \"1\" on date " + dateStr + " didn't match.", 0,
					(int) outputChanges.get("1").get(dateStr));
			assertEquals("Zone 1 time for turkey \"1\" on date " + dateStr + " didn't match.", 0,
					(long) outputTimes.get("1").get(dateStr).get("Zone 1"));
			assertEquals("Zone 2 time for turkey \"1\" on date " + dateStr + " didn't match.", DAY_MS,
					(long) outputTimes.get("1").get(dateStr).get("Zone 2"));
		}

		assertEquals("Zone changes for turkey \"0\" date 06.06.2023 didn't match.", 2,
				(int) outputChanges.get("0").get("06.06.2023"));
		assertEquals("Zone 1 time for turkey \"0\" on date 06.06.2023 didn't match.",
				TimeUtils.parseTime("13:12:46.44"), (long) outputTimes.get("0").get("06.06.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"0\" on date 06.06.2023 didn't match.",
				TimeUtils.parseTime("10:47:13.56"), (long) outputTimes.get("0").get("06.06.2023").get("Zone 2"));

		assertEquals("Zone changes for turkey \"1\" date 06.06.2023 didn't match.", 2,
				(int) outputChanges.get("1").get("06.06.2023"));
		assertEquals("Zone 1 time for turkey \"1\" on date 06.06.2023 didn't match.",
				TimeUtils.parseTime("01:24:41.02"), (long) outputTimes.get("1").get("06.06.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"1\" on date 06.06.2023 didn't match.",
				TimeUtils.parseTime("22:35:18.98"), (long) outputTimes.get("1").get("06.06.2023").get("Zone 2"));

		assertEquals("Zone changes for turkey \"0\" date 07.06.2023 didn't match.", 0,
				(int) outputChanges.get("0").get("07.06.2023"));
		assertEquals("Zone 1 time for turkey \"0\" on date 07.06.2023 didn't match.",
				TimeUtils.parseTime("05:08:38.63"), (long) outputTimes.get("0").get("07.06.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"0\" on date 07.06.2023 didn't match.", 0,
				(long) outputTimes.get("0").get("07.06.2023").get("Zone 2"));

		assertEquals("Zone changes for turkey \"1\" date 07.06.2023 didn't match.", 1,
				(int) outputChanges.get("1").get("07.06.2023"));
		assertEquals("Zone 1 time for turkey \"1\" on date 07.06.2023 didn't match.",
				TimeUtils.parseTime("01:55:47.63"), (long) outputTimes.get("1").get("07.06.2023").get("Zone 1"));
		assertEquals("Zone 2 time for turkey \"1\" on date 07.06.2023 didn't match.",
				TimeUtils.parseTime("03:12:51.00"), (long) outputTimes.get("1").get("07.06.2023").get("Zone 2"));

		Map<String, List<ZoneStay>> staysData = CSVHandler.readStaysCSV(staysIn, mappings.zones);
		staysIn.close();

		List<ZoneStay> t1Stays = new ArrayList<ZoneStay>();
		t1Stays.add(new ZoneStay("0", zones.get(1), TimeUtils.parseTime("06.05.2023", "02:05:42.59"),
				TimeUtils.parseTime("06.05.2023", "02:17:19.38")));
		t1Stays.add(new ZoneStay("0", zones.get(0), TimeUtils.parseTime("06.05.2023", "02:17:19.38"),
				TimeUtils.parseTime("06.05.2023", "05:01:08.21")));
		t1Stays.add(new ZoneStay("0", zones.get(1), TimeUtils.parseTime("06.05.2023", "05:01:08.21"),
				TimeUtils.parseTime("12.05.2023", "15:44:31.99")));
		t1Stays.add(new ZoneStay("0", zones.get(0), TimeUtils.parseTime("12.05.2023", "15:44:31.99"),
				TimeUtils.parseTime("21.05.2023", "02:35:20.83")));
		t1Stays.add(new ZoneStay("0", zones.get(1), TimeUtils.parseTime("21.05.2023", "02:35:20.83"),
				TimeUtils.parseTime("31.05.2023", "18:00:48.21")));
		t1Stays.add(new ZoneStay("0", zones.get(0), TimeUtils.parseTime("31.05.2023", "18:00:48.21"),
				TimeUtils.parseTime("06.06.2023", "03:08:27.44")));
		t1Stays.add(new ZoneStay("0", zones.get(1), TimeUtils.parseTime("06.06.2023", "03:08:27.44"),
				TimeUtils.parseTime("06.06.2023", "13:55:41.00")));
		t1Stays.add(new ZoneStay("0", zones.get(0), TimeUtils.parseTime("06.06.2023", "13:55:41.00"),
				TimeUtils.parseTime("07.06.2023", "05:08:38.63")));

		assertEquals("The number of zone stays for turkey \"0\" didn't match.", t1Stays.size(),
				staysData.get("0").size());

		for (int i = 0; i < t1Stays.size(); i++) {
			assertEquals("A zone stay for turkey \"0\" didn't match.", t1Stays.get(i), staysData.get("0").get(i));
		}

		List<ZoneStay> t2Stays = new ArrayList<ZoneStay>();
		t2Stays.add(new ZoneStay("1", zones.get(0), TimeUtils.parseTime("06.05.2023", "02:05:42.59"),
				TimeUtils.parseTime("06.05.2023", "11:29:46.41")));
		t2Stays.add(new ZoneStay("1", zones.get(1), TimeUtils.parseTime("06.05.2023", "11:29:46.41"),
				TimeUtils.parseTime("06.06.2023", "12:31:00.53")));
		t2Stays.add(new ZoneStay("1", zones.get(0), TimeUtils.parseTime("06.06.2023", "12:31:00.53"),
				TimeUtils.parseTime("06.06.2023", "13:55:41.55")));
		t2Stays.add(new ZoneStay("1", zones.get(1), TimeUtils.parseTime("06.06.2023", "13:55:41.55"),
				TimeUtils.parseTime("07.06.2023", "03:12:51.00")));
		t2Stays.add(new ZoneStay("1", zones.get(0), TimeUtils.parseTime("07.06.2023", "03:12:51.00"),
				TimeUtils.parseTime("07.06.2023", "05:08:38.63")));

		assertEquals("The number of zone stays for turkey \"1\" didn't match.", t2Stays.size(),
				staysData.get("1").size());

		for (int i = 0; i < t2Stays.size(); i++) {
			assertEquals("A zone stay for turkey \"1\" didn't match.", t2Stays.get(i), staysData.get("1").get(i));
		}
	}

	/**
	 * Compares the theoretical outputs calculated when generating the input files
	 * with the actual outputs generated by the program.
	 * 
	 * @param generated The generated, expected, results.
	 * @param parsed    The results from parsing the generated test data.
	 * @param args      The arguments to be used for validation.
	 */
	public static void validateResults(final TestData generated, final TestData parsed, Arguments args) {
		assertNotNull("The generated data to validate is null.", generated);
		assertNotNull("The parsed data to validate is null.", parsed);
		assertNotNull("The arguments to use for validation are null.", args);

		// Make sure there are no missing turkeys anywhere
		assertThat("A turkey is missing from the parsed zone times.", parsed.zoneTimes.keySet(),
				hasItems(generated.zoneTimes.keySet().toArray(new String[0])));
		assertThat("A turkey is missing from the generated zone times.", generated.zoneTimes.keySet(),
				hasItems(parsed.zoneTimes.keySet().toArray(new String[0])));

		assertThat("A turkey is missing from the parsed zone changes.", parsed.zoneChanges.keySet(),
				hasItems(generated.zoneChanges.keySet().toArray(new String[0])));
		assertThat("A turkey is missing from the generated zone changes.", generated.zoneChanges.keySet(),
				hasItems(parsed.zoneChanges.keySet().toArray(new String[0])));
		assertThat("A turkey is missing from the parsed zone stays.", parsed.zoneStays.keySet(),
				hasItems(generated.zoneStays.keySet().toArray(new String[0])));
		assertThat("A turkey is missing from the generates zone stays.", generated.zoneStays.keySet(),
				hasItems(parsed.zoneStays.keySet().toArray(new String[0])));

		Map<String, TurkeyInfo> turkeys = new HashMap<String, TurkeyInfo>();
		for (TurkeyInfo ti : parsed.turkeys) {
			turkeys.put(ti.getId(), ti);
		}

		for (final String turkey : parsed.zoneTimes.keySet()) {
			Map<String, Map<String, Long>> turkeyOutputTimes = parsed.zoneTimes.get(turkey);
			assertThat("The generated data for turkey \"" + turkey + "\" is missing a date.",
					generated.zoneTimes.get(turkey).keySet(),
					hasItems(turkeyOutputTimes.keySet().toArray(new String[0])));
			assertThat("The parsed data for turkey \"" + turkey + "\" is missing a date.", turkeyOutputTimes.keySet(),
					hasItems(generated.zoneTimes.get(turkey).keySet().toArray(new String[0])));

			// Compare output totals to calculated totals
			Map<String, Long> timeSums = new HashMap<String, Long>();
			for (final String date : turkeyOutputTimes.keySet()) {
				assertThat("A date is missing from the parsed zone changes for turkey \"" + turkey + "\".",
						parsed.zoneChanges.get(turkey).keySet(), hasItem(date));
				assertThat("A date is missing from the generated zone changes for turkey \"" + turkey + "\".",
						generated.zoneChanges.get(turkey).keySet(), hasItem(date));

				assertTrue("Zone changes for turkey \"" + turkey + "\" on day " + date + " were less than zero.",
						parsed.zoneChanges.get(turkey).get(date) >= 0);

				assertEquals("Zone changes for turkey \"" + turkey + "\" on day " + date + " didn't match.",
						generated.zoneChanges.get(turkey).get(date), parsed.zoneChanges.get(turkey).get(date));

				int dayTotal = 0;

				Map<String, Long> generatedZoneTimes = generated.zoneTimes.get(turkey).get(date);
				Map<String, Long> parsedZoneTimes = turkeyOutputTimes.get(date);

				// Parsed always contains all zones, so not all parsed zones need to exist in
				// generated data
				assertThat("Missing zone in parsed data for turkey \"" + turkey + "\" date " + date + ".",
						parsedZoneTimes.keySet(), hasItems(generatedZoneTimes.keySet().toArray(new String[0])));

				for (String zone : parsedZoneTimes.keySet()) {
					long generatedTime = 0;
					if (generatedZoneTimes.containsKey(zone)) {
						generatedTime = generatedZoneTimes.get(zone);
					}

					if (!date.equals("total")) {
						if (!timeSums.containsKey(zone)) {
							timeSums.put(zone, generatedTime);
						} else {
							timeSums.put(zone, timeSums.get(zone) + generatedTime);
						}
					}

					assertEquals(
							"Turkey \"" + turkey + "\" zone \"" + zone + "\" time for day \"" + date
									+ "\" didn't match the prediction.",
							generatedTime, (long) parsedZoneTimes.get(zone));

					dayTotal += generatedTime;
				}

				if (!date.equals("total")) {
					long dtTime = 0;
					if (parsed.downtimes != null) {
						final long dayStart = TimeUtils.parseDate(date).getTimeInMillis();
						final long dayEnd = dayStart + DAY_MS;
						for (Pair<Long, Long> dt : parsed.downtimes) {
							if (dt.getKey() >= dayStart && dt.getValue() <= dayEnd) {
								dtTime += dt.getValue() - dt.getKey();
							} else if (dt.getKey() >= dayStart && dt.getKey() <= dayEnd) {
								dtTime += dayEnd - dt.getKey();
							} else if (dt.getValue() >= dayStart && dt.getValue() <= dayEnd) {
								dtTime += dt.getValue() - dayStart;
							}
						}
					}

					// FIXME find a way to make this work with the segment before a sub 1-day
					// FIXME downtime having no data for that turkey.
					if (args.fillDays && parsed.downtimes == null && turkeys.get(turkey).getEndCal() != null
							&& TimeUtils.isSameDay(turkeys.get(turkey).getEndCal(), TimeUtils.parseDate(date))) {
						assertEquals(
								"The sum of all zone totals of turkey \"" + turkey + "\" for day \"" + date
										+ "\" didn't match the time until the turkeys end time.",
								TimeUtils.getMsOfDay(turkeys.get(turkey).getEndCal()) - dtTime, dayTotal);
					} else if (args.fillDays && parsed.downtimes == null) {
						assertEquals("The sum of all zone totals of turkey \"" + turkey + "\" for day \"" + date
								+ "\" wasn't a full day.", DAY_MS - dtTime, dayTotal);
					} else {
						assertFalse("The sum of all zone totals of turkey \"" + turkey + "\" and downtimes for day \""
								+ date + "\" was more than 24 hours.", dayTotal + dtTime > DAY_MS);
					}
				}
			}

			for (String zone : timeSums.keySet()) {
				assertEquals(
						"The sume of day zone times for turkey \"" + turkey + "\" zone \"" + zone
								+ "\" does not match its total.",
						timeSums.get(zone), turkeyOutputTimes.get("total").get(zone));
			}

			// Compare output stays with calculated stays
			if (generated.zoneStays.containsKey(turkey)) {
				assertThat("A parsed zone stay did not match a generated one.", generated.zoneStays.get(turkey),
						hasItems(parsed.zoneStays.get(turkey).toArray(new ZoneStay[0])));
				assertThat("A generated zone stay did not match a parsed one.", parsed.zoneStays.get(turkey),
						hasItems(generated.zoneStays.get(turkey).toArray(new ZoneStay[0])));
				assertEquals("The number of zone stays for turkey \"" + turkey + "\" didn't match.",
						generated.zoneStays.get(turkey).size(), parsed.zoneStays.get(turkey).size());
			}

			// Compare zone stay time sum with output total
			if (generated.zoneStays.containsKey(turkey)) {
				Map<String, Long> stayTotals = new HashMap<String, Long>();
				for (ZoneStay stay : parsed.zoneStays.get(turkey)) {
					if (stayTotals.containsKey(stay.getZone().getId())) {
						stayTotals.put(stay.getZone().getId(),
								stayTotals.get(stay.getZone().getId()) + stay.getStayTime());
					} else {
						stayTotals.put(stay.getZone().getId(), stay.getStayTime());
					}
				}

				for (String zone : stayTotals.keySet()) {
					assertEquals(
							"The sum of turkey \"" + turkey + "\" zone \"" + zone
									+ "\" stay times doesn't match the total.",
							stayTotals.get(zone), turkeyOutputTimes.get("total").get(zone));
				}

				if (turkeys.get(turkey).getEndCal() != null) {
					assertFalse("The last zone stay for turkey \"" + turkey + "\" ends after its end time.",
							parsed.zoneStays.get(turkey).get(parsed.zoneStays.get(turkey).size() - 1).getExitCal()
									.after(turkeys.get(turkey).getEndCal()));
				}

				Set<ZoneStay> uniqueStays = new HashSet<ZoneStay>();
				for (ZoneStay stay : parsed.zoneStays.get(turkey)) {
					assertTrue("The parsed stays contained a duplicate ZoneStay.", uniqueStays.add(stay));
				}
			}
		}
	}

	/**
	 * Generates and writes to two files mapping files for turkeys and zones.
	 * 
	 * @param turkeys         The number of turkeys to generate.
	 * @param zones           The number of zones to generate.
	 * @param maxTransponders The max number of transponders per turkey.
	 * @param advancedTurkeys Whether turkeys with a start zone and end time should
	 *                        be generated.
	 * @param startTime       The earliest possible end time if
	 *                        {@code advancedTurkeys} is {@code true}.<br/>
	 *                        Ignored if {@code advancedTurkeys} is {@code false}.
	 * @param endTime         The latest possible end time if
	 *                        {@code advancedTurkeys} is {@code true}. Ignored if
	 *                        {@code advancedTurkeys} is {@code false}.
	 * @param tempFolder      The {@link TempFileStreamHandler} object to use to
	 *                        create the required temporary files.
	 * @return An object containing the sets of mappings and
	 *         {@link FileInputStreamHandler FileInputStreamHandlers} to read them.
	 * @throws IOException              If writing or creating one of the temporary
	 *                                  files fails.
	 * @throws NullPointerException     If {@code tempFolder} is {@code null}.
	 * @throws IllegalArgumentException If {@code turkeys} or {@code zones} is less
	 *                                  than 1. Or if {@code advancedTurkeys} is
	 *                                  {@code true} and {@code endTime} isn't after
	 *                                  {@code startTime}.
	 */
	public static TestMappings generateTestMappings(int turkeys, int zones, int maxTransponders,
			boolean advancedTurkeys, long startTime, long endTime, TempFileStreamHandler tempFolder)
			throws IOException, NullPointerException, IllegalArgumentException {
		Pair<FileInputStreamHandler, FileOutputStreamHandler> zonesPair = tempFolder.newTempIOFile("zones.csv");
		FileOutputStreamHandler zonesOut = zonesPair.getValue();
		final TestMappings mappings = new TestMappings();
		mappings.zonesIn = zonesPair.getKey();

		mappings.zones = ZoneGenerator.generateZones(zones);
		CSVHandler.writeZonesCSV(mappings.zones, zonesOut);
		zonesOut.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> turkeysPair = tempFolder.newTempIOFile("turkeys.csv");
		FileOutputStreamHandler turkeysOut = turkeysPair.getValue();

		mappings.turkeysIn = turkeysPair.getKey();
		if (advancedTurkeys) {
			mappings.turkeys = TurkeyGenerator.generateTurkeysAdvanced((short) turkeys, maxTransponders, mappings.zones,
					startTime, endTime);
		} else {
			mappings.turkeys = TurkeyGenerator.generateTurkeys(turkeys, maxTransponders);
		}
		CSVHandler.writeTurkeyCSV(mappings.turkeys, turkeysOut);
		turkeysOut.close();

		return mappings;
	}

	/**
	 * Generates the antenna records file to use for testing, as well as expected
	 * results when parsing it.
	 * 
	 * @param mappings     The mappings files to generate test data for.
	 * @param startDate    The first date to generate data for.
	 * @param days         The number of days worth of antenna records to generate
	 *                     and use.
	 * @param args         The configuration to use to generate test data.
	 * @param continuous   Whether there should be days without records between the
	 *                     days of records.
	 * @param complete     Whether each turkey should have at least one record each
	 *                     day.
	 * @param tempFolder   The {@link TempFileStreamHandler} object to use to create
	 *                     the required temporary files.
	 * @param antennaOut   The {@link IOutputStreamHandler} to write the antenna
	 *                     data to.
	 * @param downtimesOut The {@link IOutputStreamHandler} to write downtimes
	 *                     to.<br/>
	 *                     Set to {@code null} to disable the downtimes file.
	 * @return An object containing both the generated "ideal" results, as well as
	 *         the parsed output file.
	 * @throws IOException              If reading/writing/creating a temporary file
	 *                                  fails.
	 * @throws NullPointerException     If {@code mappings}, {@code args},
	 *                                  {@code tempFolder}, or {@code antennaOut} is
	 *                                  {@code null}.
	 * @throws IllegalArgumentException If {@code days} is less than 1.
	 */
	public static TestData generateTestValues(final TestMappings mappings, final String startDate, int days,
			final Arguments args, boolean continuous, boolean complete, TempFileStreamHandler tempFolder,
			final IOutputStreamHandler antennaOut, final IOutputStreamHandler downtimesOut)
			throws IOException, NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(mappings, "The mappings to generate valies for cannot be null.");
		Objects.requireNonNull(args, "The arguments to use cannot be null.");
		Objects.requireNonNull(tempFolder, "The temporary folder to use cannot be null.");
		Objects.requireNonNull(antennaOut, "The output stream handler to write the antenna data to cannot be null.");

		if (days < 1) {
			throw new IllegalArgumentException("The days to generate cannot be less than 1.");
		}

		TestData results = AntennaDataGenerator.generateAntennaData(mappings.turkeys, mappings.zones, antennaOut,
				downtimesOut, args, startDate, days, continuous, complete);
		antennaOut.close();

		return results;
	}

	/**
	 * Parses the given input data and returns the parsed results.
	 * 
	 * @param mappings    The mappings to use to parse the antenna data.
	 * @param args        The arguments to use for parsing.
	 * @param tempFolder  The temporary folder to create the output files in.
	 * @param antennaIn   The {@link IInputStreamHandler} to read the antenna
	 *                    records from.
	 * @param downtimesIn The {@link IInputStreamHandler} to read the downtimes
	 *                    from.<br/>
	 *                    Set to {@code null} if not using a downtimes file.
	 * @return A new {@link TestData} object containing the parsed data.<br/>
	 *         Does not contain downtimes if {@code downtimesIn} isn't a
	 *         {@link FileInputStreamHandler}.
	 * @throws IOException          If reading/writing/creating a temporary file
	 *                              fails.
	 * @throws NullPointerException If one of the parameters is null.
	 */
	public static TestData generateParsedData(final TestMappings mappings, final Arguments args,
			final TempFileStreamHandler tempFolder, final IInputStreamHandler antennaIn,
			final IInputStreamHandler downtimesIn) throws IOException, NullPointerException {
		Objects.requireNonNull(mappings, "The mappings to use for parsing cannot be null.");
		Objects.requireNonNull(args, "The arguments to use cannot be null.");
		Objects.requireNonNull(tempFolder, "The temporary folder to use cannot be null.");
		Objects.requireNonNull(antennaIn, "The antenna data input cannot be null.");

		List<Pair<Long, Long>> downtimes = null;
		if (downtimesIn instanceof FileInputStreamHandler) {
			downtimes = CSVHandler.readDowntimesCSV(
					new FileInputStreamHandler(((FileInputStreamHandler) downtimesIn).getInputFile()));
		}

		final Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsPair = tempFolder.newTempIOFile("totals.csv");
		final FileOutputStreamHandler totalsOut = totalsPair.getValue();
		final FileInputStreamHandler totalsIn = totalsPair.getKey();

		final Pair<FileInputStreamHandler, FileOutputStreamHandler> staysPair = tempFolder.newTempIOFile("stays.csv");
		final FileOutputStreamHandler staysOut = staysPair.getValue();
		final FileInputStreamHandler staysIn = staysPair.getKey();
		DataHandler.handleStreams(antennaIn, mappings.turkeysIn, mappings.zonesIn, downtimesIn, totalsOut, staysOut,
				args);
		totalsOut.close();
		staysOut.close();

		final Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> outputTotals = CSVHandler
				.readTotalsCSV(totalsIn);
		totalsIn.close();
		TestData results = new TestData(outputTotals.getKey(), outputTotals.getValue(),
				CSVHandler.readStaysCSV(staysIn, mappings.zones), downtimes, mappings.turkeys, mappings.zones);
		staysIn.close();

		return results;
	}

	/**
	 * A utility class for this unit test to transfer all mappings values and their
	 * input stream handlers at once.
	 * 
	 * @author Theodor Meyer zu Hörste
	 */
	public static class TestMappings {

		/**
		 * A collection of turkeys generated for testing.
		 */
		public List<TurkeyInfo> turkeys;

		/**
		 * A list of zones generated for testing.
		 */
		public List<ZoneInfo> zones;

		/**
		 * A {@link FileInputStreamHandler} to read the turkey mappings from.
		 */
		public FileInputStreamHandler turkeysIn;

		/**
		 * A {@link FileInputStreamHandler} to read the zones mappings from.
		 */
		public FileInputStreamHandler zonesIn;

		/**
		 * Creates an empty TestMappings object.
		 */
		public TestMappings() {
		}

		/**
		 * Creates a new TestMappings object and initializes all final fields.
		 * 
		 * @param turkeys   A list of turkeys generated for testing.
		 * @param zones     A list of zones generated for testing.
		 * @param turkeysIn A {@link FileInputStreamHandler} to read the turkey mappings
		 *                  from.
		 * @param zonesIn   A {@link FileInputStreamHandler} to read the zones mappings
		 *                  from.
		 */
		public TestMappings(final List<TurkeyInfo> turkeys, final List<ZoneInfo> zones,
				final FileInputStreamHandler turkeysIn, final FileInputStreamHandler zonesIn) {
			this.turkeys = turkeys;
			this.zones = zones;
			this.turkeysIn = turkeysIn;
			this.zonesIn = zonesIn;
		}

	}

}
