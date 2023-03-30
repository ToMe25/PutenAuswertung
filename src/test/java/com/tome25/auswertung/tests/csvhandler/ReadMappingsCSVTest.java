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
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.tests.rules.ErrorLogRule;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;

import net.jcip.annotations.NotThreadSafe;

/**
 * A class containing unit tests relating to {@link CSVHandler#readMappingCSV}.
 * 
 * @author theodor
 */
@NotThreadSafe
public class ReadMappingsCSVTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Rule
	public ErrorLogRule errorLog = new ErrorLogRule();

	/**
	 * All the possible separator chars {@link CSVHandler} can parse.
	 */
	private static final char[] SEPARATOR_CHARS = { ';', ',', '\t' };

	/**
	 * A test verifying the basic functionality of
	 * {@link CSVHandler#readMappingCSV}.
	 * 
	 * Tests parsing a very simple mappings csv without a header line.<br/>
	 * Uses semicolon as the separator.
	 * 
	 * Tests keys and values with all valid character types(upper case letters,
	 * lower case letters, digits, and spaces).
	 * 
	 * @throws IOException if something goes wrong with file handling, idk
	 */
	@Test
	public void readBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("basic_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key 0;Value 1;val 2");
		out.println("test;test1;test2");
		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);

		assertNotNull("Reading a basic mappings csv returned null", pair);
		assertFalse("Basic mappings.csv representation read header line despite it starting with \"Tier\".",
				pair.getKey().containsKey("Tier"));

		assertTrue("Basic mappings.csv representation did not containg first line key.",
				pair.getKey().containsKey("Key 0"));
		assertTrue("Basic mappings.csv representation did not containg second line key.",
				pair.getKey().containsKey("test"));
		assertTrue("Basic mappings.csv didn't contain first value line first value.",
				pair.getKey().get("Key 0").contains("Value 1"));
		assertTrue("Basic mappings.csv didn't contain first value line second value.",
				pair.getKey().get("Key 0").contains("val 2"));

		assertTrue("Basic mappings.csv didn't contain first value line first value.",
				pair.getValue().containsKey("Value 1"));
		assertTrue("Basic mappings.csv didn't contain first value line second value.",
				pair.getValue().containsKey("val 2"));

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key 0", new ArrayList<String>(Arrays.asList("Value 1", "val 2")));
		refPair.getKey().put("test", new ArrayList<String>(Arrays.asList("test1", "test2")));
		refPair.getValue().put("Value 1", "Key 0");
		refPair.getValue().put("val 2", "Key 0");
		refPair.getValue().put("test1", "test");
		refPair.getValue().put("test2", "test");

		assertEquals("The result of CSVHandler.readMappingsCSV did not match what was expected.", refPair, pair);
	}

	/**
	 * A unit test verifying a part of the basic functionality of
	 * {@link CSVHandler#readMappingCSV}.
	 * 
	 * Tests parsing a slightly longer automatically generated basic mappings csv
	 * without a header line.<br/>
	 * Uses comma as the separator.<br/>
	 * Each line ends with a separator.
	 * 
	 * @throws IOException if creating a temporary file or reading/writing the input
	 *                     file fails.
	 */
	@Test
	public void readLongBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("longer_basic_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		for (int i = 0; i < 300; i++) {
			out.println(String.format("Key %1$d,Value %1$d,val %1$d,v%1$d,", i));
			refPair.getKey().put("Key " + i, Arrays.asList("Value " + i, "val " + i, "v" + i));
			refPair.getValue().put("Value " + i, "Key " + i);
			refPair.getValue().put("val " + i, "Key " + i);
			refPair.getValue().put("v" + i, "Key " + i);
		}

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);
		assertEquals("The size of the first map of the longer basic mappings csv did not match.", 300,
				pair.getKey().size());
		assertEquals("The size of the second map of the longer basic mappings csv did not match.", 900,
				pair.getValue().size());
		assertEquals("The result of parsing the longer basic mappings csv did not match.", refPair, pair);
	}

	/**
	 * A test reading an empty mappings csv.
	 * 
	 * @throws IOException if reading or creating the temporary file fails.
	 */
	@Test
	public void readEmpty() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_mappings.csv");
		FileInputStreamHandler fiin = tempFile.getKey();

		assertTrue("File input stream handle for an empty file wasn't done.", fiin.done());
		assertNull("The result of reading an empty mappings file was not null.", CSVHandler.readMappingCSV(fiin));

		errorLog.checkLine("Input file did not contain any data.", 0);
	}

	/**
	 * A unit test confirming that {@link CSVHandler#readMappingCSV} will throw a
	 * {@link NullPointerException} when given a {@code null} input.
	 * 
	 * @throws NullPointerException expected
	 */
	@Test(expected = NullPointerException.class)
	public void readNullInput() throws NullPointerException {
		CSVHandler.readMappingCSV(null);
	}

	/**
	 * A test to make sure that the {@code CSVHandler} correctly handles a single
	 * file containing different separators.
	 * 
	 * @throws IOException if something breaks.
	 */
	@Test
	public void readMixedSeparator() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("mixed_separator_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		for (int i = 0; i < 15; i++) {
			out.println(String.format("Key %1$d%2$cValue %1$d%3$cval %1$d", i,
					SEPARATOR_CHARS[i % SEPARATOR_CHARS.length], SEPARATOR_CHARS[(i + 1) % SEPARATOR_CHARS.length]));
			refPair.getKey().put("Key " + i, Arrays.asList("Value " + i, "val " + i));
			refPair.getValue().put("Value " + i, "Key " + i);
			refPair.getValue().put("val " + i, "Key " + i);
		}

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);
		assertEquals("The size of the first map of the mixed separator mappings did not match.", 15,
				pair.getKey().size());
		assertEquals("The size of the second map of the mixed separator mappings did not match.", 30,
				pair.getValue().size());
		assertEquals("The result of parsing the mixed separator mappings csv did not match.", refPair, pair);
	}

	/**
	 * Makes sure {@link CSVHandler#readMappingCSV} can handle different lines
	 * having different lengths.
	 *
	 * Uses ";" as its value separator.<br/>
	 * Lines end with a value separator.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void readMixedLength() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("mixed_length_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		for (int i = 0; i < 20; i++) {
			out.print("Key " + i + ";");
			ArrayList<String> values = new ArrayList<String>();
			for (int j = 0; j < (i / 5 + 1) * (i % 5 + 1); j++) {
				String val = "Value" + i + " " + j;
				out.print(val + ";");
				values.add(val);
				refPair.getValue().put(val, "Key " + i);
			}
			out.println();
			refPair.getKey().put("Key " + i, values);
		}

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);
		assertEquals("The size of the first map of the mixed line length mappings did not match.", 20,
				pair.getKey().size());
		assertEquals("The result of parsing the mixed line length mappings csv did not match.", refPair, pair);
	}

	/**
	 * Makes sure that lines containing an already registered key are ignored and
	 * write a log message.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void readDuplicateKey() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("duplicate_key_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key;Value 1;Value 2");
		out.println("Key;Value 3;Value 4");

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key", Arrays.asList("Value 1", "Value 2"));
		refPair.getValue().put("Value 1", "Key");
		refPair.getValue().put("Value 2", "Key");

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);
		assertFalse("The second map of the parsed mappings contains a value from the second line.",
				pair.getValue().containsKey("Value 3"));
		assertEquals("The size of the first map of the duplicate key mappings did not match.", 1, pair.getKey().size());
		assertEquals("The size of the second map of the duplicate key mappings did not match.", 2,
				pair.getValue().size());
		assertEquals("The result of parsing the duplicate key mappings csv did not match.", refPair, pair);
		errorLog.checkLine("Found duplicate entity id \"Key\". Skipping line.", 0);
	}

	/**
	 * Makes sure that already existing values are ignored when they are read again
	 * in a mappings file.
	 * 
	 * @throws IOException if reading/writing/creating the temp file fails.
	 */
	@Test
	public void readDuplicateValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("duplicate_value_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key 1;Value 1;Value 2");
		out.println("Key 2;Value 3;Value 4");
		out.println("Key 3;Value 5;Value 2");

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key 1", Arrays.asList("Value 1", "Value 2"));
		refPair.getKey().put("Key 2", Arrays.asList("Value 3", "Value 4"));
		refPair.getKey().put("Key 3", Arrays.asList("Value 5"));

		refPair.getValue().put("Value 1", "Key 1");
		refPair.getValue().put("Value 2", "Key 1");
		refPair.getValue().put("Value 3", "Key 2");
		refPair.getValue().put("Value 4", "Key 2");
		refPair.getValue().put("Value 5", "Key 3");

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);
		assertTrue("The second map did not contain the duplicate value at all.",
				pair.getValue().containsKey("Value 2"));
		assertEquals("The size of the first map of the duplicate value mappings did not match.", 3,
				pair.getKey().size());
		assertEquals("The size of the second map of the duplicate value mappings did not match.", 5,
				pair.getValue().size());
		assertEquals("The result of parsing the duplicate value mappings csv did not match.", refPair, pair);
		errorLog.checkLine("Found duplicate id \"Value 2\". Ignoring the occurrence for entity \"Key 3\".", 0);
	}

	/**
	 * Checks whether the {@link CSVHandler} correctly handles Tier and Bereich
	 * headers.<br/>
	 * Uses semicolons as separators for one test and commas for the other.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readHeader() throws IOException {
		// Check Tier header
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("tier_header_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Tier;Transponder 1;Transponder 2");
		out.println("Key 1;Value 1;Value 2");
		out.println("Key 2;Value 3;Value 4");

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key 1", Arrays.asList("Value 1", "Value 2"));
		refPair.getKey().put("Key 2", Arrays.asList("Value 3", "Value 4"));

		refPair.getValue().put("Value 1", "Key 1");
		refPair.getValue().put("Value 2", "Key 1");
		refPair.getValue().put("Value 3", "Key 2");
		refPair.getValue().put("Value 4", "Key 2");

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);
		assertFalse("The parsed mappings contained the key of the header line.", pair.getKey().containsKey("Tier"));
		assertEquals("The mappings parsed from a file with header line did not match.", refPair, pair);

		// Check Bereich header
		tempFile = tempFolder.newTempInputFile("bereich_header_mappings.csv");
		out = tempFile.getValue();
		fiin = tempFile.getKey();

		out.println("Bereich,Antenne 1,Antenne 2,Antenne 3");
		out.println("Key 1,Value 1,");
		out.println("Key 2,Value 2,Value 3,");
		out.println("Key 3,Value 4,Value 5,Value 6,");

		refPair = new Pair<Map<String, List<String>>, Map<String, String>>(new LinkedHashMap<String, List<String>>(),
				new HashMap<String, String>());
		refPair.getKey().put("Key 1", Arrays.asList("Value 1"));
		refPair.getKey().put("Key 2", Arrays.asList("Value 2", "Value 3"));
		refPair.getKey().put("Key 3", Arrays.asList("Value 4", "Value 5", "Value 6"));

		refPair.getValue().put("Value 1", "Key 1");
		refPair.getValue().put("Value 2", "Key 2");
		refPair.getValue().put("Value 3", "Key 2");
		refPair.getValue().put("Value 4", "Key 3");
		refPair.getValue().put("Value 5", "Key 3");
		refPair.getValue().put("Value 6", "Key 3");

		pair = CSVHandler.readMappingCSV(fiin);
		assertFalse("The parsed mappings contained the key of the header line.", pair.getKey().containsKey("Bereich"));
		assertEquals("The mappings parsed from a file with header line did not match.", refPair, pair);
	}

	/**
	 * Tests reading a mappings file that contains a key with no value.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file tails
	 */
	@Test
	public void readNoValueLine() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("no_value_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key 1;Value 1");
		out.println("Key 2;");
		out.println("Key 3;Value 2;Value 3");

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key 1", Arrays.asList("Value 1"));
		refPair.getKey().put("Key 3", Arrays.asList("Value 2", "Value 3"));

		refPair.getValue().put("Value 1", "Key 1");
		refPair.getValue().put("Value 2", "Key 3");
		refPair.getValue().put("Value 3", "Key 3");

		assertFalse("The parsed mappings contained a key without value.", pair.getKey().containsKey("Key 2"));
		assertEquals("The parsed mappings didn't match.", refPair, pair);

		errorLog.checkLine("Input line \"Key 2;\" did not contain at least two tokens. Skipping line.", 0);
	}

	/**
	 * Tests reading an input line containing an empty value.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readEmptyValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_value_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key 1;Value 1");
		out.println("Key 2;;Value 2");
		out.println("Key 3;Value 3;Value 4");

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key 1", Arrays.asList("Value 1"));
		refPair.getKey().put("Key 2", Arrays.asList("Value 2"));
		refPair.getKey().put("Key 3", Arrays.asList("Value 3", "Value 4"));

		refPair.getValue().put("Value 1", "Key 1");
		refPair.getValue().put("Value 2", "Key 2");
		refPair.getValue().put("Value 3", "Key 3");
		refPair.getValue().put("Value 4", "Key 3");

		assertEquals("The parsed mappings with an empty value didn't match.", refPair, pair);

		errorLog.checkLine("Found empty value in line \"Key 2;;Value 2\". Skipping.", 0);
	}

	/**
	 * Reads a mappings csv containing an invalid key.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidKey() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_key_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key 1;Value 1");
		out.println("Key #2;Value 2");
		out.println("Key 3;Value 3;Value 4");

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key 1", Arrays.asList("Value 1"));
		refPair.getKey().put("Key 3", Arrays.asList("Value 3", "Value 4"));

		refPair.getValue().put("Value 1", "Key 1");
		refPair.getValue().put("Value 3", "Key 3");
		refPair.getValue().put("Value 4", "Key 3");

		assertFalse("The parsed mappings contained the invalid key.", pair.getKey().containsKey("Key #2"));
		assertEquals("The parsed mappings didn't match.", refPair, pair);

		errorLog.checkLine("Found invalid entity id \"Key #2\". Skipping line.", 0);
	}

	/**
	 * Reads a mappings csv containing an invalid value.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_value_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key 1;Value 1");
		out.println("Key 2;Value #2;Value 3");
		out.println("Key 3;Value 4;Value 5");

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key 1", Arrays.asList("Value 1"));
		refPair.getKey().put("Key 2", Arrays.asList("Value 3"));
		refPair.getKey().put("Key 3", Arrays.asList("Value 4", "Value 5"));

		refPair.getValue().put("Value 1", "Key 1");
		refPair.getValue().put("Value 3", "Key 2");
		refPair.getValue().put("Value 4", "Key 3");
		refPair.getValue().put("Value 5", "Key 3");

		assertFalse("The parsed mappings contained the invalid value.", pair.getValue().containsKey("Value #2"));
		assertEquals("The parsed mappings didn't match.", refPair, pair);

		errorLog.checkLine("Found invalid id \"Value #2\". Skipping.", 0);
	}

	/**
	 * Reads mappings with a line that contains a value, but no valid one.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readNoValidValue() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("no_valid_value_mappings.csv");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		out.println("Key 1;Value 1");
		out.println("Key 2;Value #2");
		out.println("Key 3;Value 3;Value 4");

		Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);

		Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
				new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
		refPair.getKey().put("Key 1", Arrays.asList("Value 1"));
		refPair.getKey().put("Key 3", Arrays.asList("Value 3", "Value 4"));

		refPair.getValue().put("Value 1", "Key 1");
		refPair.getValue().put("Value 3", "Key 3");
		refPair.getValue().put("Value 4", "Key 3");

		assertFalse("The parsed mappings contained the key without valid.", pair.getKey().containsKey("Key 2"));
		assertEquals("The parsed mappings didn't match.", refPair, pair);

		errorLog.checkLine("Found invalid id \"Value #2\". Skipping.", 0);
		errorLog.checkLine("Input line \"Key 2;Value #2\" did not contain at least one valid value. Skipping line.");
	}

}
