package com.tome25.auswertung.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.DataHandler;
import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.ZoneStay;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.testdata.AntennaDataGenerator;
import com.tome25.auswertung.testdata.TurkeyGenerator;
import com.tome25.auswertung.testdata.ZoneGenerator;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * A class containing tests to verify the correctness of the generated output
 * data.
 * 
 * @author theodor
 */
public class OutputDataTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	/**
	 * A very simple test case, with no missing days, and no filling day ends.
	 * 
	 * @throws IOException If reading/writing/creating a temp file fails.
	 */
	@Test
	public void basic() throws IOException {
		TestTotals totals = generateTestValues(100, 5, 10, false, false);
		Map<String, Map<String, Map<String, Long>>> antennaTimes = totals.antennaTimes;
		Map<String, Map<String, Integer>> antennaChanges = totals.antennaChanges;
		Map<String, List<ZoneStay>> antennaStays = totals.antennaStays;
		Map<String, Map<String, Map<String, Long>>> outputTimes = totals.outputTimes;
		Map<String, Map<String, Integer>> outputChanges = totals.outputChanges;
		Map<String, List<ZoneStay>> outputStays = totals.outputStays;

		for (String turkey : antennaTimes.keySet()) {
			assertTrue("The output data is missing turkey \"" + turkey + "\".", outputTimes.containsKey(turkey));
		}

		for (String turkey : outputTimes.keySet()) {
			assertTrue("There was no zone changes count output for the turkey \"" + turkey + "\".",
					outputChanges.containsKey(turkey));
			assertTrue("There was no zone stays output for the turkey \"" + turkey + "\".",
					outputStays.containsKey(turkey));
			assertTrue("There are no generated zone times for the turkey \"" + turkey + "\".",
					antennaTimes.containsKey(turkey));
			assertTrue("There are no generated zone changes count for the turkey \"" + turkey + "\".",
					antennaChanges.containsKey(turkey));
			assertTrue("There are no generated stays for the turkey \"" + turkey + "\".",
					antennaStays.containsKey(turkey));

			assertEquals("The output dates for turkey \"" + turkey + "\" didn't match.",
					antennaTimes.get(turkey).keySet(), outputTimes.get(turkey).keySet());

			for (String date : outputTimes.get(turkey).keySet()) {
				assertTrue("Day " + date + " not found in output changes for turkey \"" + turkey + "\".",
						outputChanges.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated times for turkey \"" + turkey + "\".",
						antennaTimes.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated changes for turkey \"" + turkey + "\".",
						antennaChanges.get(turkey).containsKey(date));

				assertEquals("Zone changes for turkey \"" + turkey + "\" on day " + date + " didn't match.",
						antennaChanges.get(turkey).get(date), outputChanges.get(turkey).get(date));

				Map<String, Long> antennaZoneTimes = antennaTimes.get(turkey).get(date);
				Map<String, Long> outputZoneTimes = outputTimes.get(turkey).get(date);

				for (String zone : outputZoneTimes.keySet()) {
					long antennaTime = 0;
					if (antennaZoneTimes.containsKey(zone)) {
						antennaTime = antennaZoneTimes.get(zone);
					}

					assertEquals("Turkey \"" + turkey + "\" zone \"" + zone + "\" time for day \"" + date
							+ "\" didn't match the prediction.", antennaTime, (long) outputZoneTimes.get(zone));
				}

				for (String zone : antennaZoneTimes.keySet()) {
					assertTrue("The output is missing a zone for turkey \"" + turkey + "\".",
							outputZoneTimes.containsKey(zone));
				}
			}

			assertEquals("The number of zone stays for turkey \"" + turkey + "\" didn't match.",
					antennaStays.get(turkey).size(), outputStays.get(turkey).size());

			for (ZoneStay stay : outputStays.get(turkey)) {
				assertTrue("The stay " + stay + " did not match a generated one.",
						antennaStays.get(turkey).contains(stay));
			}
		}
	}

	/**
	 * A basic non continuous test.<br/>
	 * Non continuous means there are days in the antenna records missing.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void basicNonCont() throws IOException {
		TestTotals totals = generateTestValues(100, 5, 10, false, false);
		Map<String, Map<String, Map<String, Long>>> antennaTimes = totals.antennaTimes;
		Map<String, Map<String, Integer>> antennaChanges = totals.antennaChanges;
		Map<String, List<ZoneStay>> antennaStays = totals.antennaStays;
		Map<String, Map<String, Map<String, Long>>> outputTimes = totals.outputTimes;
		Map<String, Map<String, Integer>> outputChanges = totals.outputChanges;
		Map<String, List<ZoneStay>> outputStays = totals.outputStays;

		for (String turkey : antennaTimes.keySet()) {
			assertTrue("The output data is missing turkey \"" + turkey + "\".", outputTimes.containsKey(turkey));
		}

		for (String turkey : outputTimes.keySet()) {
			assertTrue("There was no zone changes count output for the turkey \"" + turkey + "\".",
					outputChanges.containsKey(turkey));
			assertTrue("There was no zone stays output for the turkey \"" + turkey + "\".",
					outputStays.containsKey(turkey));
			assertTrue("There are no generated zone times for the turkey \"" + turkey + "\".",
					antennaTimes.containsKey(turkey));
			assertTrue("There are no generated zone changes count for the turkey \"" + turkey + "\".",
					antennaChanges.containsKey(turkey));
			assertTrue("There are no generated stays for the turkey \"" + turkey + "\".",
					antennaStays.containsKey(turkey));

			assertEquals("The output dates for turkey \"" + turkey + "\" didn't match.",
					antennaTimes.get(turkey).keySet(), outputTimes.get(turkey).keySet());

			for (String date : outputTimes.get(turkey).keySet()) {
				assertTrue("Day " + date + " not found in output changes for turkey \"" + turkey + "\".",
						outputChanges.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated times for turkey \"" + turkey + "\".",
						antennaTimes.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated changes for turkey \"" + turkey + "\".",
						antennaChanges.get(turkey).containsKey(date));

				assertEquals("Zone changes for turkey \"" + turkey + "\" on day " + date + " didn't match.",
						antennaChanges.get(turkey).get(date), outputChanges.get(turkey).get(date));

				Map<String, Long> antennaZoneTimes = antennaTimes.get(turkey).get(date);
				Map<String, Long> outputZoneTimes = outputTimes.get(turkey).get(date);

				for (String zone : outputZoneTimes.keySet()) {
					long antennaTime = 0;
					if (antennaZoneTimes.containsKey(zone)) {
						antennaTime = antennaZoneTimes.get(zone);
					}

					assertEquals("Turkey \"" + turkey + "\" zone \"" + zone + "\" time for day \"" + date
							+ "\" didn't match the prediction.", antennaTime, (long) outputZoneTimes.get(zone));
				}

				for (String zone : antennaZoneTimes.keySet()) {
					assertTrue("The output is missing a zone for turkey \"" + turkey + "\".",
							outputZoneTimes.containsKey(zone));
				}
			}

			assertEquals("The number of zone stays for turkey \"" + turkey + "\" didn't match.",
					antennaStays.get(turkey).size(), outputStays.get(turkey).size());

			for (ZoneStay stay : outputStays.get(turkey)) {
				assertTrue("The stay " + stay + " did not match a generated one.",
						antennaStays.get(turkey).contains(stay));
			}
		}
	}

	/**
	 * A basic test without missing days and with filled day starts and ends.
	 * 
	 * @throws IOException If an input file can't be read/written/created.
	 */
	@Test
	public void fillDays() throws IOException {
		TestTotals totals = generateTestValues(100, 5, 10, true, true);
		Map<String, Map<String, Map<String, Long>>> antennaTimes = totals.antennaTimes;
		Map<String, Map<String, Integer>> antennaChanges = totals.antennaChanges;
		Map<String, List<ZoneStay>> antennaStays = totals.antennaStays;
		Map<String, Map<String, Map<String, Long>>> outputTimes = totals.outputTimes;
		Map<String, Map<String, Integer>> outputChanges = totals.outputChanges;
		Map<String, List<ZoneStay>> outputStays = totals.outputStays;

		for (String turkey : antennaTimes.keySet()) {
			assertTrue("The output data is missing turkey \"" + turkey + "\".", outputTimes.containsKey(turkey));
		}

		for (String turkey : outputTimes.keySet()) {
			assertTrue("There was no zone changes count output for the turkey \"" + turkey + "\".",
					outputChanges.containsKey(turkey));
			assertTrue("There was no zone stays output for the turkey \"" + turkey + "\".",
					outputStays.containsKey(turkey));
			assertTrue("There are no generated zone times for the turkey \"" + turkey + "\".",
					antennaTimes.containsKey(turkey));
			assertTrue("There are no generated zone changes count for the turkey \"" + turkey + "\".",
					antennaChanges.containsKey(turkey));
			assertTrue("There are no generated stays for the turkey \"" + turkey + "\".",
					antennaStays.containsKey(turkey));

			assertEquals("The output dates for turkey \"" + turkey + "\" didn't match.",
					antennaTimes.get(turkey).keySet(), outputTimes.get(turkey).keySet());

			for (String date : outputTimes.get(turkey).keySet()) {
				assertTrue("Day " + date + " not found in output changes for turkey \"" + turkey + "\".",
						outputChanges.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated times for turkey \"" + turkey + "\".",
						antennaTimes.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated changes for turkey \"" + turkey + "\".",
						antennaChanges.get(turkey).containsKey(date));

				assertEquals("Zone changes for turkey \"" + turkey + "\" on day " + date + " didn't match.",
						antennaChanges.get(turkey).get(date), outputChanges.get(turkey).get(date));

				int dayTotal = 0;

				Map<String, Long> antennaZoneTimes = antennaTimes.get(turkey).get(date);
				Map<String, Long> outputZoneTimes = outputTimes.get(turkey).get(date);

				for (String zone : outputZoneTimes.keySet()) {
					long antennaTime = 0;
					if (antennaZoneTimes.containsKey(zone)) {
						antennaTime = antennaZoneTimes.get(zone);
					}

					assertEquals("Turkey \"" + turkey + "\" zone \"" + zone + "\" time for day \"" + date
							+ "\" didn't match the prediction.", antennaTime, (long) outputZoneTimes.get(zone));

					dayTotal += antennaTime;
				}

				for (String zone : antennaZoneTimes.keySet()) {
					assertTrue("The output is missing a zone for turkey \"" + turkey + "\".",
							outputZoneTimes.containsKey(zone));
				}

				if (!date.equals("total")) {
					assertEquals("The sum of all zone totals of turkey \"" + turkey + "\" for day \"" + date
							+ "\" wasn't 24h.", 24 * 3600000, dayTotal);
				}
			}

			assertEquals("The number of zone stays for turkey \"" + turkey + "\" didn't match.",
					antennaStays.get(turkey).size(), outputStays.get(turkey).size());

			for (ZoneStay stay : outputStays.get(turkey)) {
				assertTrue("The stay " + stay + " did not match a generated one.",
						antennaStays.get(turkey).contains(stay));
			}
		}
	}

	/**
	 * A basic test with non continuous input.<br/>
	 * Fills starts and ends of days.
	 * 
	 * @throws IOException If an input file can't be read/written/created.
	 */
	@Test
	public void fillDaysNonCont() throws IOException {
		TestTotals totals = generateTestValues(100, 5, 10, false, true);
		Map<String, Map<String, Map<String, Long>>> antennaTimes = totals.antennaTimes;
		Map<String, Map<String, Integer>> antennaChanges = totals.antennaChanges;
		Map<String, List<ZoneStay>> antennaStays = totals.antennaStays;
		Map<String, Map<String, Map<String, Long>>> outputTimes = totals.outputTimes;
		Map<String, Map<String, Integer>> outputChanges = totals.outputChanges;
		Map<String, List<ZoneStay>> outputStays = totals.outputStays;

		for (String turkey : antennaTimes.keySet()) {
			assertTrue("The output data is missing turkey \"" + turkey + "\".", outputTimes.containsKey(turkey));
		}

		for (String turkey : outputTimes.keySet()) {
			assertTrue("There was no zone changes count output for the turkey \"" + turkey + "\".",
					outputChanges.containsKey(turkey));
			assertTrue("There was no zone stays output for the turkey \"" + turkey + "\".",
					outputStays.containsKey(turkey));
			assertTrue("There are no generated zone times for the turkey \"" + turkey + "\".",
					antennaTimes.containsKey(turkey));
			assertTrue("There are no generated zone changes count for the turkey \"" + turkey + "\".",
					antennaChanges.containsKey(turkey));
			assertTrue("There are no generated stays for the turkey \"" + turkey + "\".",
					antennaStays.containsKey(turkey));

			assertEquals("The output dates for turkey \"" + turkey + "\" didn't match.",
					antennaTimes.get(turkey).keySet(), outputTimes.get(turkey).keySet());

			for (String date : outputTimes.get(turkey).keySet()) {
				assertTrue("Day " + date + " not found in output changes for turkey \"" + turkey + "\".",
						outputChanges.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated times for turkey \"" + turkey + "\".",
						antennaTimes.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated changes for turkey \"" + turkey + "\".",
						antennaChanges.get(turkey).containsKey(date));

				assertEquals("Zone changes for turkey \"" + turkey + "\" on day " + date + " didn't match.",
						antennaChanges.get(turkey).get(date), outputChanges.get(turkey).get(date));

				int dayTotal = 0;

				Map<String, Long> antennaZoneTimes = antennaTimes.get(turkey).get(date);
				Map<String, Long> outputZoneTimes = outputTimes.get(turkey).get(date);

				for (String zone : outputZoneTimes.keySet()) {
					long antennaTime = 0;
					if (antennaZoneTimes.containsKey(zone)) {
						antennaTime = antennaZoneTimes.get(zone);
					}

					assertEquals("Turkey \"" + turkey + "\" zone \"" + zone + "\" time for day \"" + date
							+ "\" didn't match the prediction.", antennaTime, (long) outputZoneTimes.get(zone));

					dayTotal += antennaTime;
				}

				for (String zone : antennaZoneTimes.keySet()) {
					assertTrue("The output is missing a zone for turkey \"" + turkey + "\".",
							outputZoneTimes.containsKey(zone));
				}

				if (!date.equals("total")) {
					assertEquals("The sum of all zone totals of turkey \"" + turkey + "\" for day \"" + date
							+ "\" wasn't 24h.", 24 * 3600000, dayTotal);
				}
			}

			assertEquals("The number of zone stays for turkey \"" + turkey + "\" didn't match.",
					antennaStays.get(turkey).size(), outputStays.get(turkey).size());

			for (ZoneStay stay : outputStays.get(turkey)) {
				assertTrue("The stay " + stay + " did not match a generated one.",
						antennaStays.get(turkey).contains(stay));
			}
		}
	}

	/**
	 * A manually written antenna records file for testing zones times of less than
	 * 5 mins crossing the day border.
	 * 
	 * @throws IOException If reading/writing/creating a temp file fails.
	 */
	@Test
	public void shortCrossDate() throws IOException {
		TestMappings mappings = generateTestMappings(2, 3);
		List<TurkeyInfo> turkeys = mappings.turkeys;
		Map<String, List<String>> zones = mappings.zones;
		FileInputStreamHandler turkeysIn = mappings.turkeysIn;
		FileInputStreamHandler zonesIn = mappings.zonesIn;
		String t1 = turkeys.get(0).getTransponders().get(0);
		String t2 = turkeys.get(1).getTransponders().get(0);
		String a1 = zones.get("Zone 1").get(0);
		String a2 = zones.get("Zone 2").get(0);
		String a3 = zones.get("Zone 3").get(0);

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
		DataHandler.handleStreams(antennaIn, turkeysIn, zonesIn, totalsOut, staysOut, false);
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

		Map<String, List<ZoneStay>> staysData = CSVHandler.readStaysCSV(staysIn);
		staysIn.close();

		List<ZoneStay> t1Stays = new ArrayList<ZoneStay>();
		t1Stays.add(new ZoneStay("0", "Zone 2", TimeUtils.parseTime("06.03.2022", "02:12:45.32"),
				TimeUtils.parseTime("06.03.2022", "02:18:17.96")));
		t1Stays.add(new ZoneStay("0", "Zone 3", TimeUtils.parseTime("06.03.2022", "02:18:17.96"),
				TimeUtils.parseTime("06.03.2022", "08:58:23.48")));
		t1Stays.add(new ZoneStay("0", "Zone 1", TimeUtils.parseTime("06.03.2022", "08:58:23.48"),
				TimeUtils.parseTime("06.03.2022", "18:43:52.67")));
		t1Stays.add(new ZoneStay("0", "Zone 2", TimeUtils.parseTime("06.03.2022", "18:43:52.67"),
				TimeUtils.parseTime("06.03.2022", "23:52:45.89")));
		t1Stays.add(new ZoneStay("0", "Zone 3", TimeUtils.parseTime("06.03.2022", "23:52:45.89"),
				TimeUtils.parseTime("07.03.2022", "00:09:28.09")));
		t1Stays.add(new ZoneStay("0", "Zone 2", TimeUtils.parseTime("07.03.2022", "00:09:28.09"),
				TimeUtils.parseTime("07.03.2022", "01:10:37.73")));
		t1Stays.add(new ZoneStay("0", "Zone 3", TimeUtils.parseTime("07.03.2022", "01:10:37.73"),
				TimeUtils.parseTime("07.03.2022", "01:21:42.86")));

		assertEquals("The number of zone stays for turkey \"0\" didn't match.", t1Stays.size(),
				staysData.get("0").size());

		for (int i = 0; i < staysData.get("0").size() && i < t1Stays.size(); i++) {
			assertEquals("A zone stay for turkey \"0\" didn't match.", t1Stays.get(i), staysData.get("0").get(i));
		}

		List<ZoneStay> t2Stays = new ArrayList<ZoneStay>();
		t2Stays.add(new ZoneStay("1", "Zone 1", TimeUtils.parseTime("06.03.2022", "02:12:45.32"),
				TimeUtils.parseTime("06.03.2022", "06:46:50.64")));
		t2Stays.add(new ZoneStay("1", "Zone 2", TimeUtils.parseTime("06.03.2022", "06:46:50.64"),
				TimeUtils.parseTime("06.03.2022", "15:26:08.53")));
		t2Stays.add(new ZoneStay("1", "Zone 1", TimeUtils.parseTime("06.03.2022", "15:26:08.53"),
				TimeUtils.parseTime("07.03.2022", "00:02:26.92")));
		t2Stays.add(new ZoneStay("1", "Zone 3", TimeUtils.parseTime("07.03.2022", "00:02:26.92"),
				TimeUtils.parseTime("07.03.2022", "00:13:29.15")));
		t2Stays.add(new ZoneStay("1", "Zone 2", TimeUtils.parseTime("07.03.2022", "00:13:29.15"),
				TimeUtils.parseTime("07.03.2022", "01:19:51.21")));
		t2Stays.add(new ZoneStay("1", "Zone 1", TimeUtils.parseTime("07.03.2022", "01:19:51.21"),
				TimeUtils.parseTime("07.03.2022", "01:21:42.86")));

		assertEquals("The number of zone stays for turkey \"1\" didn't match.", t2Stays.size(),
				staysData.get("1").size());

		for (int i = 0; i < staysData.get("1").size() && i < t2Stays.size(); i++) {
			assertEquals("A zone stay for turkey \"1\" didn't match.", t2Stays.get(i), staysData.get("1").get(i));
		}
	}

	/**
	 * Generates and writes to two files mapping files for turkeys and zones.
	 * 
	 * @param turkeys The number of turkeys to generate.
	 * @param zones   The number of zones to generate.
	 * @return An object containing the sets of mappings and
	 *         {@link FileInputStreamHandler FileInputStreamHandlers} to read them.
	 * @throws IOException If reading/writing/creating one of the temporary files
	 *                     fails.
	 */
	private TestMappings generateTestMappings(int turkeys, int zones) throws IOException {
		Pair<FileInputStreamHandler, FileOutputStreamHandler> turkeysPair = tempFolder.newTempIOFile("turkeys.csv");
		FileOutputStreamHandler turkeysOut = turkeysPair.getValue();
		FileInputStreamHandler turkeysIn = turkeysPair.getKey();

		List<TurkeyInfo> tks = TurkeyGenerator.generateTurkeys(turkeys, 5);
		CSVHandler.writeTurkeyCSV(tks, turkeysOut);
		turkeysOut.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> zonesPair = tempFolder.newTempIOFile("zones.csv");
		FileOutputStreamHandler zonesOut = zonesPair.getValue();
		FileInputStreamHandler zonesIn = zonesPair.getKey();

		Map<String, List<String>> zs = ZoneGenerator.generateZones(zones);
		CSVHandler.writeZonesCSV(zs, zonesOut);
		zonesOut.close();

		return new TestMappings(tks, zs, turkeysIn, zonesIn);
	}

	/**
	 * Generates and writes mappings and totals files.
	 * 
	 * @param turkeys    The number of turkeys to generate and use for the test
	 *                   data.
	 * @param zones      The number of zones to generate and use for the test data.
	 * @param days       The number of days worth of antenna records to generate and
	 *                   use.
	 * @param continuous Whether there should be days without records between the
	 *                   days of records.
	 * @param fillDays   Whether the time before the first and after the last record
	 *                   on a day should be assumed to be spent in the first/last
	 *                   zone.
	 * @return An object containing both the generated "ideal" results, as well as
	 *         the parsed output file.
	 * @throws IOException If reading/writing/creating a temporary file failed.
	 */
	private TestTotals generateTestValues(int turkeys, int zones, int days, boolean continuous, boolean fillDays)
			throws IOException {
		TestMappings mappings = generateTestMappings(turkeys, zones);

		Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder.newTempIOFile("antenna.csv");
		FileOutputStreamHandler antennaOut = antennaPair.getValue();
		FileInputStreamHandler antennaIn = antennaPair.getKey();
		Pair<Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>>, Map<String, List<ZoneStay>>> antennaData = AntennaDataGenerator
				.generateAntennaData(mappings.turkeys, mappings.zones, antennaOut, days, continuous, fillDays);
		antennaOut.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsPair = tempFolder.newTempIOFile("totals.csv");
		FileOutputStreamHandler totalsOut = totalsPair.getValue();
		FileInputStreamHandler totalsIn = totalsPair.getKey();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> staysPair = tempFolder.newTempIOFile("stays.csv");
		FileOutputStreamHandler staysOut = staysPair.getValue();
		FileInputStreamHandler staysIn = staysPair.getKey();
		DataHandler.handleStreams(antennaIn, mappings.turkeysIn, mappings.zonesIn, totalsOut, staysOut, fillDays);
		totalsOut.close();
		staysOut.close();

		Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> outputTotals = CSVHandler
				.readTotalsCSV(totalsIn);
		totalsIn.close();

		Map<String, List<ZoneStay>> outputStays = CSVHandler.readStaysCSV(staysIn);
		staysIn.close();

		return new TestTotals(antennaData.getKey().getKey(), outputTotals.getKey(), antennaData.getKey().getValue(),
				outputTotals.getValue(), antennaData.getValue(), outputStays);
	}

	/**
	 * A utility class for this unit test to transfer all mappings values and their
	 * input stream handlers at once.
	 * 
	 * @author theodor
	 */
	private class TestMappings {

		/**
		 * A collection of turkeys generated for testing.
		 */
		public final List<TurkeyInfo> turkeys;

		/**
		 * A list of zones generated for testing.
		 */
		public final Map<String, List<String>> zones;

		/**
		 * A {@link FileInputStreamHandler} to read the turkey mappings from.
		 */
		public final FileInputStreamHandler turkeysIn;

		/**
		 * A {@link FileInputStreamHandler} to read the zones mappings from.
		 */
		public final FileInputStreamHandler zonesIn;

		/**
		 * Creates a new TestMappings object and initializes all final fields.
		 * 
		 * @param turkeys   A collection of turkeys generated for testing.
		 * @param zones     A list of zones generated for testing.
		 * @param turkeysIn A {@link FileInputStreamHandler} to read the turkey mappings
		 *                  from.
		 * @param zonesIn   A {@link FileInputStreamHandler} to read the zones mappings
		 *                  from.
		 */
		public TestMappings(List<TurkeyInfo> turkeys, Map<String, List<String>> zones, FileInputStreamHandler turkeysIn,
				FileInputStreamHandler zonesIn) {
			this.turkeys = turkeys;
			this.zones = zones;
			this.turkeysIn = turkeysIn;
			this.zonesIn = zonesIn;
		}

	}

	/**
	 * A utility class for this unit test to transfer all totals values at once.
	 * 
	 * @author theodor
	 */
	private class TestTotals {

		/**
		 * Generated times per zone per day per turkey.
		 */
		public final Map<String, Map<String, Map<String, Long>>> antennaTimes;

		/**
		 * Time per zone per day per turkey read from the output file.
		 */
		public final Map<String, Map<String, Map<String, Long>>> outputTimes;

		/**
		 * Generated zone change counts.
		 */
		public final Map<String, Map<String, Integer>> antennaChanges;

		/**
		 * Zone change counts parsed from the output file.
		 */
		public final Map<String, Map<String, Integer>> outputChanges;

		/**
		 * Generated zone stays per turkey.
		 */
		public final Map<String, List<ZoneStay>> antennaStays;

		/**
		 * Zone stays per turkey parsed from the output file.
		 */
		public final Map<String, List<ZoneStay>> outputStays;

		/**
		 * Creates a new TestTotals object and initializes all final fields.
		 * 
		 * @param antennaTimes   Calculated times per turkey per day per zone.
		 * @param outputTimes    Time per turkey per day per zone parsed from the output
		 *                       file.
		 * @param antennaChanges Calculated zone changes per turkey.
		 * @param outputChanges  Zone changes per turkey parsed from the output file.
		 */
		public TestTotals(Map<String, Map<String, Map<String, Long>>> antennaTimes,
				Map<String, Map<String, Map<String, Long>>> outputTimes,
				Map<String, Map<String, Integer>> antennaChanges, Map<String, Map<String, Integer>> outputChanges,
				Map<String, List<ZoneStay>> antennaStays, Map<String, List<ZoneStay>> outputStays) {
			this.antennaTimes = antennaTimes;
			this.outputTimes = outputTimes;
			this.antennaChanges = antennaChanges;
			this.outputChanges = outputChanges;
			this.antennaStays = antennaStays;
			this.outputStays = outputStays;
		}
	}

}
