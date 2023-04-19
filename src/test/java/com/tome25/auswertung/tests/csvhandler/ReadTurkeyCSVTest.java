package com.tome25.auswertung.tests.csvhandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.tests.rules.ErrorLogRule;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.TimeUtils;

import net.jcip.annotations.NotThreadSafe;

/**
 * A class containing unit tests relating to {@link CSVHandler#readTurkeyCSV}.
 * 
 * @author theodor
 */
@NotThreadSafe
public class ReadTurkeyCSVTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Rule
	public ErrorLogRule errorLog = new ErrorLogRule();

	/**
	 * All the possible separator chars {@link CSVHandler} can parse.
	 */
	private static final char[] SEPARATOR_CHARS = { ';', ',', '\t' };

	/**
	 * A test verifying the basic functionality of {@link CSVHandler#readTurkeyCSV}.
	 * 
	 * Tests parsing a very simple turkey csv without a header line.<br/>
	 * Uses semicolon as the separator.
	 * 
	 * Tests keys and values with all valid character types(upper case letters,
	 * lower case letters, digits, and spaces).
	 * 
	 * @throws IOException if something goes wrong with file handling, idk
	 */
	@Test
	public void readBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("basic_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 0;;;;Value 1;val 2");
		out.println("test;;;;test1;test2");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertNotNull("Reading a basic turkey csv returned null.", pair);

		assertTrue("Basic turkeys.csv representation did not containg first line key.",
				pair.getKey().containsKey("Turkey 0"));
		assertTrue("Basic turkeys.csv representation did not containg second line key.",
				pair.getKey().containsKey("test"));
		assertTrue("Basic turkeys.csv didn't contain first value line first value.",
				pair.getKey().get("Turkey 0").getTransponders().contains("Value 1"));
		assertTrue("Basic turkeys.csv didn't contain first value line second value.",
				pair.getKey().get("Turkey 0").getTransponders().contains("val 2"));

		assertTrue("Basic turkeys.csv didn't contain first value line first value.",
				pair.getValue().containsKey("Value 1"));
		assertTrue("Basic turkeys.csv didn't contain first value line second value.",
				pair.getValue().containsKey("val 2"));

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 0", new TurkeyInfo("Turkey 0", Arrays.asList("Value 1", "val 2"), null, null, null,
				null, null, Arguments.empty()));
		refPair.getKey().put("test", new TurkeyInfo("test", Arrays.asList("test1", "test2"), null, null, null, null,
				null, Arguments.empty()));
		refPair.getValue().put("Value 1", "Turkey 0");
		refPair.getValue().put("val 2", "Turkey 0");
		refPair.getValue().put("test1", "test");
		refPair.getValue().put("test2", "test");

		assertEquals("The result of CSVHandler.readTurkeyCSV did not match what was expected.", refPair, pair);
	}

	/**
	 * A unit test verifying a part of the basic functionality of
	 * {@link CSVHandler#readTurkeyCSV}.
	 * 
	 * Tests parsing a slightly longer automatically generated basic turkey csv
	 * without a header line.<br/>
	 * Uses comma as the separator.<br/>
	 * Each line ends with a separator.
	 * 
	 * @throws IOException if creating a temporary file or reading/writing the input
	 *                     file fails.
	 */
	@Test
	public void readLongBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("longer_basic_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		for (int i = 0; i < 300; i++) {
			out.println(String.format("Turkey %1$d,,,,Value %1$d,val %1$d,v%1$d,", i));
			refPair.getKey().put("Turkey " + i, new TurkeyInfo("Turkey " + i,
					Arrays.asList("Value " + i, "val " + i, "v" + i), null, null, null, null, null, Arguments.empty()));
			refPair.getValue().put("Value " + i, "Turkey " + i);
			refPair.getValue().put("val " + i, "Turkey " + i);
			refPair.getValue().put("v" + i, "Turkey " + i);
		}

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertEquals("The size of the first map of the longer basic turkey csv did not match.", 300,
				pair.getKey().size());
		assertEquals("The size of the second map of the longer basic turkey csv did not match.", 900,
				pair.getValue().size());
		assertEquals("The result of parsing the longer basic turkey csv did not match.", refPair, pair);
	}

	/**
	 * Test whether parsing a turkey csv with start zones works as expected.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readStartZone() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("start_zone_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;Start 1;;;Transponder 1");
		out.println("Turkey 2;Start 2;;;Transponder 2;Transponder 3");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertNotNull("Reading a turkey csv with start zones returned null.", pair);

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Transponder 1"), null, "Start 1",
				null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Transponder 2", "Transponder 3"),
				null, "Start 2", null, null, null, Arguments.empty()));

		refPair.getValue().put("Transponder 1", "Turkey 1");
		refPair.getValue().put("Transponder 2", "Turkey 2");
		refPair.getValue().put("Transponder 3", "Turkey 2");

		assertEquals("The parsed turkeys didn't match.", refPair, pair);
	}

	/**
	 * Test parsing a turkey csv with end times but without end dates.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEndTime() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("end_time_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;12:05:13.56;;Transponder 1;Transponder 2");
		out.println("Turkey 2;;00:12:51.12;;Transponder 3;Transponder 4");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertNotNull("Reading a turkey csv with end times returned null.", pair);

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Transponder 1", "Transponder 2"),
				null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Transponder 3", "Transponder 4"),
				null, null, null, null, null, Arguments.empty()));

		refPair.getValue().put("Transponder 1", "Turkey 1");
		refPair.getValue().put("Transponder 2", "Turkey 1");
		refPair.getValue().put("Transponder 3", "Turkey 2");
		refPair.getValue().put("Transponder 4", "Turkey 2");

		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Found end time \"12:05:13.56\" without end date for turkey \"Turkey 1\". Ignoring.", 0);
		errorLog.checkLine("Found end time \"00:12:51.12\" without end date for turkey \"Turkey 2\". Ignoring.");
	}

	/**
	 * Test parsing a turkey csv with end dates but without end times.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEndDate() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("end_date_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;12.02.2022;Transponder 1;Transponder 2");
		out.println("Turkey 2;;;31.12.2022;Transponder 3");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertNotNull("Reading a turkey csv with end times returned null.", pair);

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Transponder 1", "Transponder 2"),
				null, null, null, null, TimeUtils.parseDate("12.02.2022"), Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Transponder 3"), null, null, null,
				null, TimeUtils.parseDate("31.12.2022"), Arguments.empty()));

		refPair.getValue().put("Transponder 1", "Turkey 1");
		refPair.getValue().put("Transponder 2", "Turkey 1");
		refPair.getValue().put("Transponder 3", "Turkey 2");

		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine(
				"Found end date \"12.02.2022\" without end time for turkey \"Turkey 1\". Removing turkey at beginning of the day.",
				0);
		errorLog.checkLine(
				"Found end date \"31.12.2022\" without end time for turkey \"Turkey 2\". Removing turkey at beginning of the day.");
	}

	/**
	 * Test parsing a turkey csv with end times and dates.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEndDateAndTime() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("end_date_and_time_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;12:00:00.00;15.03.2023;Transponder 1");
		out.println("Turkey 2;;23:41:36.12;21.09.2023;Transponder 2;Transponder 3");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertNotNull("Reading a turkey csv with end times returned null.", pair);

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Transponder 1"), null, null, null,
				null, TimeUtils.parseTime("15.03.2023", "12:00:00.00"), Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Transponder 2", "Transponder 3"),
				null, null, null, null, TimeUtils.parseTime("21.09.2023", "23:41:36.12"), Arguments.empty()));

		refPair.getValue().put("Transponder 1", "Turkey 1");
		refPair.getValue().put("Transponder 2", "Turkey 2");
		refPair.getValue().put("Transponder 3", "Turkey 2");

		assertEquals("The parsed turkeys didn't match.", refPair, pair);
	}

	/**
	 * Test reading end times with a decimal comma with and without and end
	 * date.<br/>
	 * Uses commas as value separators as well.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEndTimeDecimalComma() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("end_time_decimal_comma_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1,,13:07:54,71,12.12.2012,Transponder 1,Transponder 2");
		out.println("Turkey 2,,20:15:07,12,,Transponder 3,Transponder 4");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertNotNull("Reading a turkey csv with end times returned null.", pair);

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Transponder 1", "Transponder 2"),
				null, null, null, null, TimeUtils.parseTime("12.12.2012", "13:07:54.71"), Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Transponder 3", "Transponder 4"),
				null, null, null, null, null, Arguments.empty()));

		refPair.getValue().put("Transponder 1", "Turkey 1");
		refPair.getValue().put("Transponder 2", "Turkey 1");
		refPair.getValue().put("Transponder 3", "Turkey 2");
		refPair.getValue().put("Transponder 4", "Turkey 2");

		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Found end time \"20:15:07,12\" without end date for turkey \"Turkey 2\". Ignoring.", 0);
	}

	/**
	 * Checks whether the {@link CSVHandler} correctly handles Tier headers.<br/>
	 * Uses semicolons as separators.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readHeader() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("tier_header_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Tier;Start Bereich;End Zeit;End Datum;Transponder 1;Transponder 2");
		out.println("Turkey 1;;;;Value 1;Value 2");
		out.println("Turkey 2;;;;Value 3;Value 4");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Value 1", "Value 2"), null, null,
				null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Value 3", "Value 4"), null, null,
				null, null, null, Arguments.empty()));

		refPair.getValue().put("Value 1", "Turkey 1");
		refPair.getValue().put("Value 2", "Turkey 1");
		refPair.getValue().put("Value 3", "Turkey 2");
		refPair.getValue().put("Value 4", "Turkey 2");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertFalse("The parsed turkeys contained the key of the header line.", pair.getKey().containsKey("Tier"));
		assertEquals("The turkeys parsed from a file with header line did not match.", refPair, pair);
	}

	/**
	 * A test reading an empty turkey csv.
	 * 
	 * @throws IOException if reading or creating the temporary file fails.
	 */
	@Test
	public void readEmpty() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_turkeys.csv");
		FileInputStreamHandler fiin = tempFile.getKey();

		assertTrue("File input stream handle for an empty file wasn't done.", fiin.done());
		assertNull("The result of reading an empty turkey file was not null.",
				CSVHandler.readTurkeyCSV(fiin, Arguments.empty()));

		errorLog.checkLine("Input file did not contain any data.", 0);
	}

	/**
	 * A unit test confirming that {@link CSVHandler#readTurkeyCSV} will throw a
	 * {@link NullPointerException} when given a {@code null} input.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void readNullInput() throws NullPointerException {
		CSVHandler.readTurkeyCSV(null, Arguments.empty());
	}

	/**
	 * A unit test confirming that {@link CSVHandler#readTurkeyCSV} throws a
	 * {@link NullPointerException} when given a {@code null} {@link Arguments}
	 * instance.
	 * 
	 * @throws NullPointerException Always.
	 * @throws IOException          When creating the temp file fails.
	 */
	@Test(expected = NullPointerException.class)
	public void readNullArgs() throws NullPointerException, IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("null_args_turkeys.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		CSVHandler.readTurkeyCSV(fiin, null);
	}

	/**
	 * A test to make sure that the {@code CSVHandler} correctly handles a single
	 * file containing different separators.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void readMixedSeparator() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("mixed_separator_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		for (int i = 0; i < 15; i++) {
			out.println(String.format("Turkey %1$d%2$c%3$c%4$c%5$cValue %1$d%6$cval %1$d", i,
					SEPARATOR_CHARS[i % SEPARATOR_CHARS.length], SEPARATOR_CHARS[(i + 1) % SEPARATOR_CHARS.length],
					SEPARATOR_CHARS[(i + 2) % SEPARATOR_CHARS.length],
					SEPARATOR_CHARS[(i + 3) % SEPARATOR_CHARS.length],
					SEPARATOR_CHARS[(i + 4) % SEPARATOR_CHARS.length]));
			refPair.getKey().put("Turkey " + i, new TurkeyInfo("Turkey " + i, Arrays.asList("Value " + i, "val " + i),
					null, null, null, null, null, Arguments.empty()));
			refPair.getValue().put("Value " + i, "Turkey " + i);
			refPair.getValue().put("val " + i, "Turkey " + i);
		}

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertEquals("The size of the first map of the mixed separator turkey did not match.", 15,
				pair.getKey().size());
		assertEquals("The size of the second map of the mixed separator turkey did not match.", 30,
				pair.getValue().size());
		assertEquals("The result of parsing the mixed separator turkey csv did not match.", refPair, pair);
	}

	/**
	 * Makes sure {@link CSVHandler#readTurkeyCSV} can handle different lines having
	 * different lengths.
	 *
	 * Uses ";" as its value separator.<br/>
	 * Lines end with a value separator.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void readMixedLength() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("mixed_length_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		for (int i = 0; i < 20; i++) {
			out.print("Turkey " + i + ";;;;");
			ArrayList<String> values = new ArrayList<String>();
			for (int j = 0; j < (i / 5 + 1) * (i % 5 + 1); j++) {
				String val = "Value" + i + " " + j;
				out.print(val + ";");
				values.add(val);
				refPair.getValue().put(val, "Turkey " + i);
			}
			out.println();
			refPair.getKey().put("Turkey " + i,
					new TurkeyInfo("Turkey " + i, values, null, null, null, null, null, Arguments.empty()));
		}

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertEquals("The size of the first map of the mixed line length turkey did not match.", 20,
				pair.getKey().size());
		assertEquals("The result of parsing the mixed line length turkey csv did not match.", refPair, pair);
	}

	/**
	 * Makes sure that lines containing an already registered key are ignored and
	 * write a log message.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void readDuplicateKey() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("duplicate_key_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key;;;;Value 1;Value 2");
		out.println("Key;;;;Value 3;Value 4");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Key", new TurkeyInfo("Key", Arrays.asList("Value 1", "Value 2"), null, null, null, null,
				null, Arguments.empty()));
		refPair.getValue().put("Value 1", "Key");
		refPair.getValue().put("Value 2", "Key");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertFalse("The second map of the parsed turkeys contains a value from the second line.",
				pair.getValue().containsKey("Value 3"));
		assertEquals("The size of the first map of the duplicate key turkeys did not match.", 1, pair.getKey().size());
		assertEquals("The size of the second map of the duplicate key turkeys did not match.", 2,
				pair.getValue().size());
		assertEquals("The result of parsing the duplicate key turkey csv did not match.", refPair, pair);
		errorLog.checkLine("Found duplicate turkey id \"Key\". Skipping line.", 0);
	}

	/**
	 * Makes sure that already existing values are ignored when they are read again
	 * in a turkey file.
	 * 
	 * @throws IOException if reading/writing/creating the temp file fails.
	 */
	@Test
	public void readDuplicateValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("duplicate_value_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;;Value 1;Value 2");
		out.println("Turkey 2;;;;Value 3;Value 4");
		out.println("Turkey 3;;;;Value 5;Value 2");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Value 1", "Value 2"), null, null,
				null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Value 3", "Value 4"), null, null,
				null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3",
				new TurkeyInfo("Turkey 3", Arrays.asList("Value 5"), null, null, null, null, null, Arguments.empty()));

		refPair.getValue().put("Value 1", "Turkey 1");
		refPair.getValue().put("Value 2", "Turkey 1");
		refPair.getValue().put("Value 3", "Turkey 2");
		refPair.getValue().put("Value 4", "Turkey 2");
		refPair.getValue().put("Value 5", "Turkey 3");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());
		assertTrue("The second map did not contain the duplicate value at all.",
				pair.getValue().containsKey("Value 2"));
		assertEquals("The size of the first map of the duplicate value turkeys did not match.", 3,
				pair.getKey().size());
		assertEquals("The size of the second map of the duplicate value turkeys did not match.", 5,
				pair.getValue().size());
		assertEquals("The result of parsing the duplicate value turkey csv did not match.", refPair, pair);
		errorLog.checkLine("Found duplicate id \"Value 2\". Ignoring the occurrence for turkey \"Turkey 3\".", 0);
	}

	/**
	 * Tests reading a turkey file that contains a key with no value.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file tails
	 */
	@Test
	public void readNoValueLine() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("no_value_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;;Value 1");
		out.println("Turkey 2;;;;");
		out.println("Turkey 3;;;;Value 2;Value 3");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1",
				new TurkeyInfo("Turkey 1", Arrays.asList("Value 1"), null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3", new TurkeyInfo("Turkey 3", Arrays.asList("Value 2", "Value 3"), null, null,
				null, null, null, Arguments.empty()));

		refPair.getValue().put("Value 1", "Turkey 1");
		refPair.getValue().put("Value 2", "Turkey 3");
		refPair.getValue().put("Value 3", "Turkey 3");

		assertFalse("The parsed turkeys contained a key without value.", pair.getKey().containsKey("Turkey 2"));
		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Input line \"Turkey 2;;;;\" did not contain at least five tokens. Skipping line.", 0);
	}

	/**
	 * Tests reading a turkey file that contains only a key.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file tails
	 */
	@Test
	public void readKeyOnlyLine() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("key_only_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;;Value 1");
		out.println("Turkey 2");
		out.println("Turkey 3;;;;Value 2");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1",
				new TurkeyInfo("Turkey 1", Arrays.asList("Value 1"), null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3",
				new TurkeyInfo("Turkey 3", Arrays.asList("Value 2"), null, null, null, null, null, Arguments.empty()));

		refPair.getValue().put("Value 1", "Turkey 1");
		refPair.getValue().put("Value 2", "Turkey 3");

		assertFalse("The parsed turkeys contained a key without value.", pair.getKey().containsKey("Turkey 2"));
		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Input line \"Turkey 2\" did not contain at least five tokens. Skipping line.", 0);
	}

	/**
	 * Tests reading an input line containing an empty value.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readEmptyValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_value_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;;Value 1");
		out.println("Turkey 2;;;;;Value 2");
		out.println("Turkey 3;;;;Value 3;Value 4");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1",
				new TurkeyInfo("Turkey 1", Arrays.asList("Value 1"), null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 2",
				new TurkeyInfo("Turkey 2", Arrays.asList("Value 2"), null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3", new TurkeyInfo("Turkey 3", Arrays.asList("Value 3", "Value 4"), null, null,
				null, null, null, Arguments.empty()));

		refPair.getValue().put("Value 1", "Turkey 1");
		refPair.getValue().put("Value 2", "Turkey 2");
		refPair.getValue().put("Value 3", "Turkey 3");
		refPair.getValue().put("Value 4", "Turkey 3");

		assertEquals("The parsed turkeys with an empty value didn't match.", refPair, pair);

		errorLog.checkLine("Found empty value in line \"Turkey 2;;;;;Value 2\". Skipping.", 0);
	}

	/**
	 * Reads a turkey csv containing an invalid key.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidKey() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_key_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;;Value 1");
		out.println("Turkey #2;;;;Value 2");
		out.println("Turkey 3;;;;Value 3;Value 4");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1",
				new TurkeyInfo("Turkey 1", Arrays.asList("Value 1"), null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3", new TurkeyInfo("Turkey 3", Arrays.asList("Value 3", "Value 4"), null, null,
				null, null, null, Arguments.empty()));

		refPair.getValue().put("Value 1", "Turkey 1");
		refPair.getValue().put("Value 3", "Turkey 3");
		refPair.getValue().put("Value 4", "Turkey 3");

		assertFalse("The parsed turkeys contained the invalid key.", pair.getKey().containsKey("Turkey #2"));
		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Found invalid turkey id \"Turkey #2\". Skipping line.", 0);
	}

	/**
	 * Reads a turkey csv containing an invalid starting zone.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidStartZone() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("invalid_start_zone_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;Start 1;;;Transponder 1;Transponder 2");
		out.println("Turkey 2;Start #2;;;Transponder 3");
		out.println("Turkey 3;Start 3;;;Transponder 4");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Transponder 1", "Transponder 2"),
				null, "Start 1", null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3", new TurkeyInfo("Turkey 3", Arrays.asList("Transponder 4"), null, "Start 3",
				null, null, null, Arguments.empty()));

		refPair.getValue().put("Transponder 1", "Turkey 1");
		refPair.getValue().put("Transponder 2", "Turkey 1");
		refPair.getValue().put("Transponder 4", "Turkey 3");

		assertFalse("The parsed turkeys contained the turkey with an invalid start zone.",
				pair.getKey().containsKey("Turkey 2"));
		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Found invalid start zone id \"Start #2\" for turkey \"Turkey 2\". Skipping line.", 0);
	}

	/**
	 * Test parsing various lines with invalid end times.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidEndTime() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("invalid_end_time_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;15:12:01.12;;Transponder 1;Transponder 2");
		out.println("Turkey 2;;25:13:23.71;;Transponder 3");
		out.println("Turkey 3;;12:00 PM;;Transponder 4");
		out.println("Turkey 4;;24:66:00.00;01.01.2023;Transponder 5;Transponder 6");
		out.println("Turkey 5;;13:45 AM;02.01.2023;Transponder 7");
		out.println("Turkey 6;;09:07:37.51;03.01.2023;Transponder 8");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Transponder 1", "Transponder 2"),
				null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Transponder 3"), null, null, null,
				null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3", new TurkeyInfo("Turkey 3", Arrays.asList("Transponder 4"), null, null, null,
				null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 4", new TurkeyInfo("Turkey 4", Arrays.asList("Transponder 5", "Transponder 6"),
				null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 5", new TurkeyInfo("Turkey 5", Arrays.asList("Transponder 7"), null, null, null,
				null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 6", new TurkeyInfo("Turkey 6", Arrays.asList("Transponder 8"), null, null, null,
				null, TimeUtils.parseTime("03.01.2023", "09:07:37.51"), Arguments.empty()));

		refPair.getValue().put("Transponder 1", "Turkey 1");
		refPair.getValue().put("Transponder 2", "Turkey 1");
		refPair.getValue().put("Transponder 3", "Turkey 2");
		refPair.getValue().put("Transponder 4", "Turkey 3");
		refPair.getValue().put("Transponder 5", "Turkey 4");
		refPair.getValue().put("Transponder 6", "Turkey 4");
		refPair.getValue().put("Transponder 7", "Turkey 5");
		refPair.getValue().put("Transponder 8", "Turkey 6");

		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Found end time \"15:12:01.12\" without end date for turkey \"Turkey 1\". Ignoring.", 0);
		errorLog.checkLine("Found end time \"25:13:23.71\" without end date for turkey \"Turkey 2\". Ignoring.");
		errorLog.checkLine("Found end time \"12:00 PM\" without end date for turkey \"Turkey 3\". Ignoring.");
		errorLog.checkLine(
				"Failed to parse end time \"24:66:00.00\" or end date \"01.01.2023\" for turkey \"Turkey 4\". Ignoring end time and date.");
		errorLog.checkLine(
				"Failed to parse end time \"13:45 AM\" or end date \"02.01.2023\" for turkey \"Turkey 5\". Ignoring end time and date.");
	}

	/**
	 * Test parsing a turkey csv containing invalid end dates.
	 * 
	 * @throws IOException If reading/writign/creating the temp file fails.
	 */
	@Test
	public void readInvalidEndDate() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("invalid_end_date_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;15.08.2022;Transponder 1");
		out.println("Turkey 2;;;26.13.2022;Transponder 2;Transponder 3");
		out.println("Turkey 3;;00:07:15.00;12#2022;Transponder 4;Transponder 5");
		out.println("Turkey 4;;14:27:59.06;01.10.2022;Transponder 6");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1", new TurkeyInfo("Turkey 1", Arrays.asList("Transponder 1"), null, null, null,
				null, TimeUtils.parseDate("15.08.2022"), Arguments.empty()));
		refPair.getKey().put("Turkey 2", new TurkeyInfo("Turkey 2", Arrays.asList("Transponder 2", "Transponder 3"),
				null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3", new TurkeyInfo("Turkey 3", Arrays.asList("Transponder 4", "Transponder 5"),
				null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 4", new TurkeyInfo("Turkey 4", Arrays.asList("Transponder 6"), null, null, null,
				null, TimeUtils.parseTime("01.10.2022", "14:27:59.06"), Arguments.empty()));

		refPair.getValue().put("Transponder 1", "Turkey 1");
		refPair.getValue().put("Transponder 2", "Turkey 2");
		refPair.getValue().put("Transponder 3", "Turkey 2");
		refPair.getValue().put("Transponder 4", "Turkey 3");
		refPair.getValue().put("Transponder 5", "Turkey 3");
		refPair.getValue().put("Transponder 6", "Turkey 4");

		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine(
				"Found end date \"15.08.2022\" without end time for turkey \"Turkey 1\". Removing turkey at beginning of the day.",
				0);
		errorLog.checkLine("Failed to parse end date \"26.13.2022\" for turkey \"Turkey 2\". Ignoring.");
		errorLog.checkLine(
				"Failed to parse end time \"00:07:15.00\" or end date \"12#2022\" for turkey \"Turkey 3\". Ignoring end time and date.");
	}

	/**
	 * Reads a turkey csv containing an invalid value.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_value_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;;Value 1");
		out.println("Turkey 2;;;;Value #2;Value 3");
		out.println("Turkey 3;;;;Value 4;Value 5");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1",
				new TurkeyInfo("Turkey 1", Arrays.asList("Value 1"), null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 2",
				new TurkeyInfo("Turkey 2", Arrays.asList("Value 3"), null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3", new TurkeyInfo("Turkey 3", Arrays.asList("Value 4", "Value 5"), null, null,
				null, null, null, Arguments.empty()));

		refPair.getValue().put("Value 1", "Turkey 1");
		refPair.getValue().put("Value 3", "Turkey 2");
		refPair.getValue().put("Value 4", "Turkey 3");
		refPair.getValue().put("Value 5", "Turkey 3");

		assertFalse("The parsed turkeys contained the invalid value.", pair.getValue().containsKey("Value #2"));
		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Found invalid id \"Value #2\" for turkey \"Turkey 2\". Skipping.", 0);
	}

	/**
	 * Reads turkey with a line that contains a value, but no valid one.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readNoValidValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("no_valid_value_turkeys.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Turkey 1;;;;Value 1");
		out.println("Turkey 2;;;;Value #2");
		out.println("Turkey 3;;;;Value 3;Value 4");

		Pair<Map<String, TurkeyInfo>, Map<String, String>> pair = CSVHandler.readTurkeyCSV(fiin, Arguments.empty());

		Pair<Map<String, TurkeyInfo>, Map<String, String>> refPair = new Pair<Map<String, TurkeyInfo>, Map<String, String>>(
				new LinkedHashMap<String, TurkeyInfo>(), new HashMap<String, String>());
		refPair.getKey().put("Turkey 1",
				new TurkeyInfo("Turkey 1", Arrays.asList("Value 1"), null, null, null, null, null, Arguments.empty()));
		refPair.getKey().put("Turkey 3", new TurkeyInfo("Turkey 3", Arrays.asList("Value 3", "Value 4"), null, null,
				null, null, null, Arguments.empty()));

		refPair.getValue().put("Value 1", "Turkey 1");
		refPair.getValue().put("Value 3", "Turkey 3");
		refPair.getValue().put("Value 4", "Turkey 3");

		assertFalse("The parsed turkeys contained the key without valid.", pair.getKey().containsKey("Turkey 2"));
		assertEquals("The parsed turkeys didn't match.", refPair, pair);

		errorLog.checkLine("Found invalid id \"Value #2\" for turkey \"Turkey 2\". Skipping.", 0);
		errorLog.checkLine(
				"Input line \"Turkey 2;;;;Value #2\" did not contain at least one valid value. Skipping line.");
	}

}
