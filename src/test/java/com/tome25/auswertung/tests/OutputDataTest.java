package com.tome25.auswertung.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
		Arguments args = Arguments.empty();
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, true, true, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A simple test using a downtimes file.
	 * 
	 * @throws IOException If reading/writing/creating a temp file fails.
	 */
	@Test
	public void downtimes() throws IOException {
		Arguments args = Arguments.empty();
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> downtimesPair = tempFolder
				.newTempIOFile("downtimes.csv");
		final TestData generated = generateTestValues(mappings, 10, args, true, true, tempFolder,
				antennaPair.getValue(), downtimesPair.getValue());
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(),
				downtimesPair.getKey());
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic non continuous test.<br/>
	 * Non continuous means there are days in the antenna records missing.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void basicNonCont() throws IOException {
		Arguments args = Arguments.empty();
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, false, true, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic non continuous test without a min zone stay time.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void basicNoMinTime() throws IOException {
		Arguments args = Arguments.empty();
		args.minTime = 0;
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, false, true, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic non continuous test with a min zone stay time of 30 minutes.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void basic30mMinTime() throws IOException {
		Arguments args = Arguments.empty();
		args.minTime = 30 * 60;
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, false, true, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic test in which not every turkey has a record each day.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void basicIncomplete() throws IOException {
		Arguments args = Arguments.empty();
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, true, false, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic non continuous test in which not every turkey has a record each day.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void basicIncompleteNonCont() throws IOException {
		Arguments args = Arguments.empty();
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, false, false, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic test without missing days and with filled day starts and ends.
	 * 
	 * @throws IOException If an input file can't be read/written/created.
	 */
	@Test
	public void fillDays() throws IOException {
		Arguments args = Arguments.empty();
		args.fillDays = true;
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, true, true, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic test with non continuous input.<br/>
	 * Fills starts and ends of days.
	 * 
	 * @throws IOException If an input file can't be read/written/created.
	 */
	@Test
	public void fillDaysNonCont() throws IOException {
		Arguments args = Arguments.empty();
		args.fillDays = true;
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, false, true, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic non continuous test without a min zone stay time filling day starts
	 * and ends.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void fillDaysNoMinTime() throws IOException {
		Arguments args = Arguments.empty();
		args.fillDays = true;
		args.minTime = 0;
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, false, true, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic non continuous test with a min zone stay time of 30 minutes filling
	 * day starts and ends.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void fillDays30mMinTime() throws IOException {
		Arguments args = Arguments.empty();
		args.fillDays = true;
		args.minTime = 30 * 60;
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, false, true, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic test in which not every turkey has a record each day.<br/>
	 * Fills day starts and ends.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void fillDaysIncomplete() throws IOException {
		Arguments args = Arguments.empty();
		args.fillDays = true;
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, true, false, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A basic non continuous test in which not every turkey has a record each
	 * day.<br/>
	 * Fills day starts and ends.
	 * 
	 * @throws IOException If reading/writing/creating a temp file failed.
	 */
	@Test
	public void fillDaysIncompleteNonCont() throws IOException {
		Arguments args = Arguments.empty();
		args.fillDays = true;
		final TestMappings mappings = generateTestMappings(100, 5, tempFolder);
		final Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder
				.newTempIOFile("antenna.csv");
		final TestData generated = generateTestValues(mappings, 10, args, false, false, tempFolder,
				antennaPair.getValue(), null);
		final TestData parsed = generateParsedData(mappings, args, tempFolder, antennaPair.getKey(), null);
		validateResults(generated, parsed, args);
	}

	/**
	 * A manually written antenna records file for testing zones times of less than
	 * 5 mins crossing the day border.
	 * 
	 * @throws IOException If reading/writing/creating a temp file fails.
	 */
	@Test
	public void shortCrossDate() throws IOException {
		TestMappings mappings = generateTestMappings(2, 3, tempFolder);
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
	 * Compares the theoretical outputs calculated when generating the input files
	 * with the actual outputs generated by the program.
	 * 
	 * @param generated The generated, expected, results.
	 * @param parsed    The results from parsing the generated test data.
	 * @param args      The arguments to be used for validation.
	 */
	public static void validateResults(final TestData generated, final TestData parsed, Arguments args)
			throws NullPointerException {
		assertNotNull("The generated data to validate is null.", generated);
		assertNotNull("The parsed data to validate is null.", parsed);
		assertNotNull("The arguments to use for validation are null.", args);

		for (String turkey : generated.zoneTimes.keySet()) {
			assertTrue("The output data is missing turkey \"" + turkey + "\".", parsed.zoneTimes.containsKey(turkey));
		}

		for (String turkey : parsed.zoneTimes.keySet()) {
			assertTrue("There was no zone changes count output for the turkey \"" + turkey + "\".",
					parsed.zoneChanges.containsKey(turkey));
			assertTrue("There was no zone stays output for the turkey \"" + turkey + "\".",
					parsed.zoneChanges.containsKey(turkey));
			assertTrue("There are no generated zone times for the turkey \"" + turkey + "\".",
					generated.zoneTimes.containsKey(turkey));
			assertTrue("There are no generated zone changes count for the turkey \"" + turkey + "\".",
					generated.zoneChanges.containsKey(turkey));
			assertTrue("There are no generated stays for the turkey \"" + turkey + "\".",
					generated.zoneStays.containsKey(turkey));

			Map<String, Map<String, Long>> turkeyOutputTimes = parsed.zoneTimes.get(turkey);
			assertEquals("The output dates for turkey \"" + turkey + "\" didn't match.",
					generated.zoneTimes.get(turkey).keySet(), turkeyOutputTimes.keySet());

			// Compare output totals to calculated totals
			for (String date : turkeyOutputTimes.keySet()) {
				assertTrue("Day " + date + " not found in output changes for turkey \"" + turkey + "\".",
						parsed.zoneChanges.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated times for turkey \"" + turkey + "\".",
						generated.zoneTimes.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated changes for turkey \"" + turkey + "\".",
						generated.zoneChanges.get(turkey).containsKey(date));

				assertEquals("Zone changes for turkey \"" + turkey + "\" on day " + date + " didn't match.",
						generated.zoneChanges.get(turkey).get(date), parsed.zoneChanges.get(turkey).get(date));

				int dayTotal = 0;

				Map<String, Long> generatedZoneTimes = generated.zoneTimes.get(turkey).get(date);
				Map<String, Long> outputZoneTimes = turkeyOutputTimes.get(date);

				for (String zone : outputZoneTimes.keySet()) {
					long generatedTime = 0;
					if (generatedZoneTimes.containsKey(zone)) {
						generatedTime = generatedZoneTimes.get(zone);
					}

					assertEquals(
							"Turkey \"" + turkey + "\" zone \"" + zone + "\" time for day \"" + date
									+ "\" didn't match the prediction.",
							generatedTime, (long) outputZoneTimes.get(zone));

					dayTotal += generatedTime;
				}

				for (String zone : generatedZoneTimes.keySet()) {
					assertTrue("The output is missing a zone for turkey \"" + turkey + "\".",
							outputZoneTimes.containsKey(zone));
				}

				if (args.fillDays && !date.equals("total")) {
					assertEquals("The sum of all zone totals of turkey \"" + turkey + "\" for day \"" + date
							+ "\" wasn't 24h.", 24 * 3600000, dayTotal);
				}
			}

			// Compare output stays with calculated stays
			assertEquals("The number of zone stays for turkey \"" + turkey + "\" didn't match.",
					generated.zoneStays.get(turkey).size(), parsed.zoneStays.get(turkey).size());

			Set<ZoneStay> generatedTurkeyStays = new HashSet<ZoneStay>(generated.zoneStays.get(turkey));
			Map<String, Long> stayTotals = new HashMap<String, Long>();
			for (ZoneStay stay : parsed.zoneStays.get(turkey)) {
				assertTrue("The stay " + stay + " did not match a generated one.", generatedTurkeyStays.contains(stay));

				// Compare zone stay time sum with output total
				if (stayTotals.containsKey(stay.getZone())) {
					stayTotals.put(stay.getZone(), stayTotals.get(stay.getZone()) + stay.getStayTime());
				} else {
					stayTotals.put(stay.getZone(), stay.getStayTime());
				}
			}

			for (String zone : stayTotals.keySet()) {
				assertEquals(
						"The sum of turkey \"" + turkey + "\" zone \"" + zone
								+ "\" stay times doesn't match the total.",
						stayTotals.get(zone), turkeyOutputTimes.get("total").get(zone));
			}
		}
	}

	/**
	 * Generates and writes to two files mapping files for turkeys and zones.
	 * 
	 * @param turkeys    The number of turkeys to generate.
	 * @param zones      The number of zones to generate.
	 * @param tempFolder The {@link TempFileStreamHandler} object to use to create
	 *                   the required temporary files.
	 * @return An object containing the sets of mappings and
	 *         {@link FileInputStreamHandler FileInputStreamHandlers} to read them.
	 * @throws IOException              If reading/writing/creating one of the
	 *                                  temporary files fails.
	 * @throws NullPointerException     If {@code tempFolder} is {@code null}.
	 * @throws IllegalArgumentException If {@code turkeys} or {@code zones} is less
	 *                                  than 1.
	 */
	public static TestMappings generateTestMappings(int turkeys, int zones, TempFileStreamHandler tempFolder)
			throws IOException, NullPointerException, IllegalArgumentException {
		Pair<FileInputStreamHandler, FileOutputStreamHandler> turkeysPair = tempFolder.newTempIOFile("turkeys.csv");
		FileOutputStreamHandler turkeysOut = turkeysPair.getValue();

		final TestMappings mappings = new TestMappings();
		mappings.turkeysIn = turkeysPair.getKey();
		mappings.turkeys = TurkeyGenerator.generateTurkeys(turkeys, 5);
		CSVHandler.writeTurkeyCSV(mappings.turkeys, turkeysOut);
		turkeysOut.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> zonesPair = tempFolder.newTempIOFile("zones.csv");
		FileOutputStreamHandler zonesOut = zonesPair.getValue();
		mappings.zonesIn = zonesPair.getKey();

		mappings.zones = ZoneGenerator.generateZones(zones);
		CSVHandler.writeZonesCSV(mappings.zones, zonesOut);
		zonesOut.close();

		return mappings;
	}

	/**
	 * Generates the antenna records file to use for testing, as well as expected
	 * results when parsing it.
	 * 
	 * @param mappings     The mappings files to generate test data for.
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
	public static TestData generateTestValues(final TestMappings mappings, int days, final Arguments args,
			boolean continuous, boolean complete, TempFileStreamHandler tempFolder,
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
				downtimesOut, args, days, continuous, complete);
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
			CSVHandler.readDowntimesCSV(
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
				CSVHandler.readStaysCSV(staysIn), downtimes, mappings.turkeys, mappings.zones);
		staysIn.close();

		return results;
	}

	/**
	 * A utility class for this unit test to transfer all mappings values and their
	 * input stream handlers at once.
	 * 
	 * @author theodor
	 */
	public static class TestMappings {

		/**
		 * A collection of turkeys generated for testing.
		 */
		public List<TurkeyInfo> turkeys;

		/**
		 * A list of zones generated for testing.
		 */
		public Map<String, List<String>> zones;

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
		 * @param turkeys   A collection of turkeys generated for testing.
		 * @param zones     A list of zones generated for testing.
		 * @param turkeysIn A {@link FileInputStreamHandler} to read the turkey mappings
		 *                  from.
		 * @param zonesIn   A {@link FileInputStreamHandler} to read the zones mappings
		 *                  from.
		 */
		public TestMappings(final List<TurkeyInfo> turkeys, final Map<String, List<String>> zones,
				final FileInputStreamHandler turkeysIn, final FileInputStreamHandler zonesIn) {
			this.turkeys = turkeys;
			this.zones = zones;
			this.turkeysIn = turkeysIn;
			this.zonesIn = zonesIn;
		}

	}

}
