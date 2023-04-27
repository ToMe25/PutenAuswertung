package com.tome25.auswertung.tests.csvhandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.ZoneInfo;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.tests.rules.ErrorLogRule;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;

import net.jcip.annotations.NotThreadSafe;

/**
 * A class containing unit tests relating to {@link CSVHandler#readZonesCSV}.
 * 
 * @author Theodor Meyer zu Hörste
 */
@NotThreadSafe
public class ReadZonesCSVTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Rule
	public ErrorLogRule errorLog = new ErrorLogRule();

	/**
	 * All the possible separator chars {@link CSVHandler} can parse.
	 */
	private static final char[] SEPARATOR_CHARS = { ';', ',', '\t' };

	/**
	 * A test verifying the basic functionality of {@link CSVHandler#readZonesCSV}.
	 * 
	 * Tests parsing a very simple zones csv without a header line.<br/>
	 * Uses semicolon as the separator.
	 * 
	 * Tests keys and values with all valid character types(upper case letters,
	 * lower case letters, digits, spaces, and hyphens).
	 * 
	 * @throws IOException if something goes wrong with file handling, idk
	 */
	@Test
	public void readBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("basic_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Zone - 0;Antenna 1;ant-2");
		out.println("test;test1;test2");

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);
		assertNotNull("Reading a basic zones csv returned null.", zones);

		assertTrue("Basic zones.csv didn't contain first line first value.", zones.containsKey("Antenna 1"));
		assertTrue("Basic zones.csv didn't contain first value line second value.", zones.containsKey("ant-2"));
		assertTrue("Basic zones.csv didn't contain second line first value.", zones.containsKey("test1"));
		assertTrue("Basic zones.csv didn't contain second value line second value.", zones.containsKey("test2"));

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone - 0", true, "Antenna 1", "ant-2"));
		refMap.put("ant-2", refMap.get("Antenna 1"));
		refMap.put("test1", new ZoneInfo("test", true, "test1", "test2"));
		refMap.put("test2", refMap.get("test1"));

		assertEquals("The result of CSVHandler.readZonesCSV did not match what was expected.", refMap, zones);
	}

	/**
	 * A unit test verifying a part of the basic functionality of
	 * {@link CSVHandler#readZonesCSV}.
	 * 
	 * Tests parsing a slightly longer automatically generated basic zones csv
	 * without a header line.<br/>
	 * Uses comma as the separator.<br/>
	 * Each line ends with a separator.
	 * 
	 * @throws IOException if creating a temporary file or reading/writing the input
	 *                     file fails.
	 */
	@Test
	public void readLongBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("longer_basic_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		for (int i = 0; i < 300; i++) {
			out.println(String.format("Zone %1$d,Value %1$d,val %1$d,v%1$d,", i));
			ZoneInfo zi = new ZoneInfo("Zone " + i, true, "Value " + i, "val " + i, "v" + i);
			refMap.put("Value " + i, zi);
			refMap.put("val " + i, zi);
			refMap.put("v" + i, zi);
		}

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);
		assertEquals("The size of the longer basic zones csv map did not match.", 900, zones.size());
		assertEquals("The result of parsing the longer basic zones csv did not match.", refMap, zones);
	}

	/**
	 * Checks whether the {@link CSVHandler} correctly handles Bereich headers.<br/>
	 * Uses commas as separators.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readHeader() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("bereich_header_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Bereich,Antenne 1,Antenne 2,Antenne 3");
		out.println("Zone 1,Antenna 1,");
		out.println("Zone 2,Antenna 2,Antenna 3,");
		out.println("Zone 3,Antenna 4,Antenna 5,Antenna 6,");

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);
		assertFalse("The parsed zones contained a value of the header line.", zones.containsKey("Antenne 1"));

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone 1", true, "Antenna 1"));
		refMap.put("Antenna 2", new ZoneInfo("Zone 2", true, "Antenna 2", "Antenna 3"));
		refMap.put("Antenna 3", refMap.get("Antenna 2"));
		refMap.put("Antenna 4", new ZoneInfo("Zone 3", true, "Antenna 4", "Antenna 5", "Antenna 6"));
		refMap.put("Antenna 5", refMap.get("Antenna 4"));
		refMap.put("Antenna 6", refMap.get("Antenna 4"));

		assertEquals("The zones parsed from a file with header line did not match.", refMap, zones);
	}

	/**
	 * A test reading an empty zones csv.
	 * 
	 * @throws IOException if reading or creating the temporary file fails.
	 */
	@Test
	public void readEmpty() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_zones.csv");
		FileInputStreamHandler fiin = tempFile.getKey();

		assertTrue("File input stream handle for an empty file wasn't done.", fiin.done());
		assertNull("The result of reading an empty zones file was not null.", CSVHandler.readZonesCSV(fiin));

		errorLog.checkLine("Input file did not contain any data.", 0);
	}

	/**
	 * A unit test confirming that {@link CSVHandler#readZonesCSV} will throw a
	 * {@link NullPointerException} when given a {@code null} input.
	 * 
	 * @throws NullPointerException expected
	 */
	@Test(expected = NullPointerException.class)
	public void readNullInput() throws NullPointerException {
		CSVHandler.readZonesCSV(null);
	}

	/**
	 * A test to make sure that the {@code CSVHandler} correctly handles a single
	 * file containing different separators.
	 * 
	 * @throws IOException if something breaks.
	 */
	@Test
	public void readMixedSeparator() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("mixed_separator_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		for (int i = 0; i < 15; i++) {
			out.println(String.format("Zone %1$d%2$cValue %1$d%3$cval %1$d", i,
					SEPARATOR_CHARS[i % SEPARATOR_CHARS.length], SEPARATOR_CHARS[(i + 1) % SEPARATOR_CHARS.length]));
			ZoneInfo zi = new ZoneInfo("Zone " + i, true, "Value " + i, "val " + i);
			refMap.put("Value " + i, zi);
			refMap.put("val " + i, zi);
		}

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);
		assertEquals("The size of the mixed separator zones map did not match.", 30, zones.size());
		assertEquals("The result of parsing the mixed separator zones csv did not match.", refMap, zones);
	}

	/**
	 * Makes sure {@link CSVHandler#readZonesCSV} can handle different lines having
	 * different lengths.
	 *
	 * Uses ";" as its value separator.<br/>
	 * Lines end with a value separator.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void readMixedLength() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("mixed_length_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		int size = 0;
		for (int i = 0; i < 20; i++) {
			out.print("Zone " + i + ";");
			ArrayList<String> values = new ArrayList<String>();
			for (int j = 0; j < (i / 5 + 1) * (i % 5 + 1); j++) {
				String val = "Value" + i + " " + j;
				out.print(val + ";");
				values.add(val);
				size++;
			}
			out.println();
			ZoneInfo zi = new ZoneInfo("Zone " + i, true, values);
			for (String value : values) {
				refMap.put(value, zi);
			}
		}

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);
		assertEquals("The size the mixed line length zones map did not match.", size, zones.size());
		assertEquals("The result of parsing the mixed line length zones csv did not match.", refMap, zones);
	}

	/**
	 * Makes sure that lines containing an already registered key are ignored and
	 * write a log message.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void readDuplicateKey() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("duplicate_key_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Zone;Antenna 1;Antenna 2");
		out.println("Zone;Antenna 3;Antenna 4");

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone", true, "Antenna 1", "Antenna 2"));
		refMap.put("Antenna 2", refMap.get("Antenna 1"));

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);
		assertFalse("The map of the parsed zones contains a value from the second line.",
				zones.containsKey("Antenna 3"));
		assertEquals("The size of the duplicate key zones map did not match.", 2, zones.size());
		assertEquals("The result of parsing the duplicate key zones csv did not match.", refMap, zones);
		errorLog.checkLine("Found duplicate zone id \"Zone\". Skipping line.", 0);
	}

	/**
	 * Makes sure that already existing values are ignored when they are read again
	 * in a zones file.
	 * 
	 * @throws IOException if reading/writing/creating the temp file fails.
	 */
	@Test
	public void readDuplicateValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("duplicate_value_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Zone 1;Antenna 1;Antenna 2");
		out.println("Zone 2;Antenna 3;Antenna 4");
		out.println("Zone 3;Antenna 5;Antenna 2");

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone 1", true, "Antenna 1", "Antenna 2"));
		refMap.put("Antenna 2", refMap.get("Antenna 1"));
		refMap.put("Antenna 3", new ZoneInfo("Zone 2", true, "Antenna 3", "Antenna 4"));
		refMap.put("Antenna 4", refMap.get("Antenna 3"));
		refMap.put("Antenna 5", new ZoneInfo("Zone 3", true, "Antenna 5"));

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);
		assertTrue("The second map did not contain the duplicate value at all.", zones.containsKey("Antenna 2"));
		assertEquals("The size of the duplicate value zones map did not match.", 5, zones.size());
		assertEquals("The result of parsing the duplicate value zones csv did not match.", refMap, zones);
		errorLog.checkLine("Found duplicate id \"Antenna 2\". Ignoring the occurrence for zone \"Zone 3\".", 0);
	}

	/**
	 * Tests reading a zones file that contains a key with no value.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file tails
	 */
	@Test
	public void readNoValueLine() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("no_value_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Zone 1;Antenna 1");
		out.println("Zone 2;");
		out.println("Zone 3;Antenna 2;Antenna 3");

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone 1", true, "Antenna 1"));
		refMap.put("Antenna 2", new ZoneInfo("Zone 3", true, "Antenna 2", "Antenna 3"));
		refMap.put("Antenna 3", refMap.get("Antenna 2"));

		assertEquals("The size of the no value line map didn't match.", 3, zones.size());
		assertEquals("The parsed zones didn't match.", refMap, zones);

		errorLog.checkLine("Input line \"Zone 2;\" did not contain at least two tokens. Skipping line.", 0);
	}

	/**
	 * Tests reading an input line containing an empty value.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readEmptyValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_value_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Zone 1;Antenna 1");
		out.println("Zone 2;;Antenna 2");
		out.println("Zone 3;Antenna 3;Antenna 4");

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone 1", true, "Antenna 1"));
		refMap.put("Antenna 2", new ZoneInfo("Zone 2", true, "Antenna 2"));
		refMap.put("Antenna 3", new ZoneInfo("Zone 3", true, "Antenna 3", "Antenna 4"));
		refMap.put("Antenna 4", refMap.get("Antenna 3"));

		assertEquals("The parsed zones with an empty value didn't match.", refMap, zones);

		errorLog.checkLine("Found empty value in line \"Zone 2;;Antenna 2\". Skipping.", 0);
	}

	/**
	 * Reads a zones csv containing an invalid key.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidKey() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_key_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Zone 1;Antenna 1");
		out.println("Zone #2;Antenna 2");
		out.println("Zone 3;Antenna 3;Antenna 4");

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone 1", true, "Antenna 1"));
		refMap.put("Antenna 3", new ZoneInfo("Zone 3", true, "Antenna 3", "Antenna 4"));
		refMap.put("Antenna 4", refMap.get("Antenna 3"));

		assertFalse("The parsed zones contained a value from the invalid key.", zones.containsKey("Key #2"));
		assertEquals("The parsed zones didn't match.", refMap, zones);

		errorLog.checkLine("Found invalid zone id \"Zone #2\". Skipping line.", 0);
	}

	/**
	 * Reads a zones csv containing an invalid value.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_value_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Zone 1;Antenna 1");
		out.println("Zone 2;Antenna #2;Antenna 3");
		out.println("Zone 3;Antenna 4;Antenna 5");

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone 1", true, "Antenna 1"));
		refMap.put("Antenna 3", new ZoneInfo("Zone 2", true, "Antenna 3"));
		refMap.put("Antenna 4", new ZoneInfo("Zone 3", true, "Antenna 4", "Antenna 5"));
		refMap.put("Antenna 5", refMap.get("Antenna 4"));

		assertFalse("The parsed zones contained the invalid value.", zones.containsKey("Antenna #2"));
		assertEquals("The parsed zones didn't match.", refMap, zones);

		errorLog.checkLine("Found invalid id \"Antenna #2\" for zone \"Zone 2\". Skipping.", 0);
	}

	/**
	 * Reads zones with a line that contains a value, but no valid one.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readNoValidValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("no_valid_value_zones.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Zone 1;Antenna 1");
		out.println("Zone 2;Antenna #2");
		out.println("Zone 3;Antenna 3;Antenna 4");

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(fiin);

		Map<String, ZoneInfo> refMap = new HashMap<String, ZoneInfo>();
		refMap.put("Antenna 1", new ZoneInfo("Zone 1", true, "Antenna 1"));
		refMap.put("Antenna 3", new ZoneInfo("Zone 3", true, "Antenna 3", "Antenna 4"));
		refMap.put("Antenna 4", refMap.get("Antenna 3"));

		assertEquals("The size of the no valid value line map didn't match.", 3, zones.size());
		assertEquals("The parsed zones didn't match.", refMap, zones);

		errorLog.checkLine("Found invalid id \"Antenna #2\" for zone \"Zone 2\". Skipping.", 0);
		errorLog.checkLine("Input line \"Zone 2;Antenna #2\" did not contain at least one valid value. Skipping line.");
	}

}
