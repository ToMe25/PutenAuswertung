package com.tome25.auswertung;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.utils.Pair;

/**
 * A class containing unit tests related to the {@link CSVHandler} class.
 * 
 * @author theodor
 */
public class CSVHandlerTests {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static final char[] SEPARATOR_CHARS = { ';', ',', '\t' };

	/**
	 * A test verifying the basic functionality of
	 * {@link CSVHandler#readMappingCSV}.
	 * 
	 * Tests parsing a very simple mappings csv with a header line.<br/>
	 * Uses semicolon as the separator.
	 * 
	 * @throws IOException if something goes wrong with file handling, idk
	 */
	@Test
	public void readBasicMappingCSV() throws IOException {
		File inputFile = tempFolder.newFile("basic_mappings.csv");
		try (PrintStream out = new PrintStream(inputFile);
				FileInputStreamHandler fiin = new FileInputStreamHandler(inputFile)) {
			out.println("Tier;Value 1 Header;Value 2 Header");
			out.println("Key;Value 1;val 2");
			out.println("test;test1;test2");
			Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);

			assertNotNull("Reading a basic mappings csv returned null", pair);
			assertFalse("Basic mappings.csv representation read header line despite it starting with \"Tier\".",
					pair.getKey().containsKey("Tier"));

			assertTrue("Basic mappings.csv representation did not containg first line key.",
					pair.getKey().containsKey("Key"));
			assertTrue("Basic mappings.csv representation did not containg second line key.",
					pair.getKey().containsKey("test"));
			assertTrue("Basic mappings.csv didn't contain first value line first value.",
					pair.getKey().get("Key").contains("Value 1"));
			assertTrue("Basic mappings.csv didn't contain first value line second value.",
					pair.getKey().get("Key").contains("val 2"));

			assertTrue("Basic mappings.csv didn't contain first value line first value.",
					pair.getValue().containsKey("Value 1"));
			assertTrue("Basic mappings.csv didn't contain first value line second value.",
					pair.getValue().containsKey("val 2"));

			Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
					new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
			refPair.getKey().put("Key", new ArrayList<String>(Arrays.asList("Value 1", "val 2")));
			refPair.getKey().put("test", new ArrayList<String>(Arrays.asList("test1", "test2")));
			refPair.getValue().put("Value 1", "Key");
			refPair.getValue().put("val 2", "Key");
			refPair.getValue().put("test1", "test");
			refPair.getValue().put("test2", "test");

			assertEquals("The result of CSVHandler.readMappingsCSV did not match what was expected.", refPair, pair);
		}
	}

	/**
	 * A unit test verifying a part of the basic functionality of
	 * {@link CSVHandler#readMappingCSV}.
	 * 
	 * Tests parsing a slightly longer automatically generated basic mappings
	 * csv.<br/>
	 * Uses comma as the separator.<br/>
	 * Each line ends with a separator.
	 * 
	 * @throws IOException if creating a temp file or reading/writing the input file
	 *                     fails.
	 */
	@Test
	public void readLongerBasicMappings() throws IOException {
		File inputFile = tempFolder.newFile("longer_basic_mappings.csv");

		try (PrintStream out = new PrintStream(inputFile);
				FileInputStreamHandler fiin = new FileInputStreamHandler(inputFile)) {
			Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
					new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
			for (int i = 0; i < 15; i++) {
				out.println(String.format("Key %1$d,Value %1$d,val %1$d,v%1$d,", i));
				refPair.getKey().put("Key " + i, Arrays.asList("Value " + i, "val " + i, "v" + i));
				refPair.getValue().put("Value " + i, "Key " + i);
				refPair.getValue().put("val " + i, "Key " + i);
				refPair.getValue().put("v" + i, "Key " + i);
			}

			Pair<Map<String, List<String>>, Map<String, String>> pair = CSVHandler.readMappingCSV(fiin);
			assertEquals("The size of the first map of the longer basic mappings csv did not match.", 15,
					pair.getKey().size());
			assertEquals("The size of the second map of the longer basic mappings csv did not match.", 45,
					pair.getValue().size());
			assertEquals("The result of parsing the longer basic mappings csv did not match.", refPair, pair);
		}
	}

	/**
	 * A unit test confirming that {@link CSVHandler#readMappingCSV} will throw a
	 * {@link NullPointerException} when given a {@code null} input.
	 * 
	 * @throws NullPointerException expected
	 */
	@Test
	public void testMappingsNullInput() throws NullPointerException {
		thrown.expect(NullPointerException.class);
		CSVHandler.readMappingCSV(null);
	}

	/**
	 * A test to make sure that the {@code CSVHandler} correctly handles a single
	 * file containing different separators.
	 * 
	 * @throws IOException if something breaks.
	 */
	@Test
	public void readMixedMappingsSeparator() throws IOException {
		File inputFile = tempFolder.newFile("mixed_separator_mappings.csv");

		try (PrintStream out = new PrintStream(inputFile);
				FileInputStreamHandler fiin = new FileInputStreamHandler(inputFile)) {
			out.println("Tier;Antenne 1,Antenne 2");

			Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
					new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
			for (int i = 0; i < 15; i++) {
				out.println(String.format("Key %1$d%2$cValue %1$d%3$cval %1$d", i,
						SEPARATOR_CHARS[i % SEPARATOR_CHARS.length],
						SEPARATOR_CHARS[(i + 1) % SEPARATOR_CHARS.length]));
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
	}

	/**
	 * Makes sure {@link CSVHandler#readMappingCSV} can handle different lines
	 * having different lengths.<br/>
	 * Has a header line.<br/>
	 * Uses ";" as its value separator.<br/>
	 * Lines end with a value separator.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void readMixedLengths() throws IOException {
		File inputFile = tempFolder.newFile("mixed_len_mappings.csv");

		try (PrintStream out = new PrintStream(inputFile);
				FileInputStreamHandler fiin = new FileInputStreamHandler(inputFile)) {
			out.println("Bereich,test1,test2");

			Pair<Map<String, List<String>>, Map<String, String>> refPair = new Pair<Map<String, List<String>>, Map<String, String>>(
					new LinkedHashMap<String, List<String>>(), new HashMap<String, String>());
			for (int i = 0; i < 20; i++) {
				out.print("Key " + i + ";");
				ArrayList<String> values = new ArrayList<String>();
				for (int j = 0; j < (i / 5 + 1) * (i % 5 + 1); j++) {
					String val = "Value" + i + ":" + j;
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
	}

	/**
	 * Makes sure that lines containing an already registered key are ignored and
	 * write a log message.
	 * 
	 * @throws IOException if creating/reading/writing the temporary file fails.
	 */
	@Test
	public void mappingsDuplicateKey() throws IOException {
		File inputFile = tempFolder.newFile("duplicate_key_mappings.csv");

		PrintStream oldErr = LogHandler.getError();
		try (PrintStream out = new PrintStream(inputFile);
				FileInputStreamHandler fiin = new FileInputStreamHandler(inputFile);
				ByteArrayOutputStream barr = new ByteArrayOutputStream()) {
			LogHandler.setError(new PrintStream(barr));
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
			assertEquals("The size of the first map of the duplicate key mappings did not match.", 1,
					pair.getKey().size());
			assertEquals("The size of the second map of the duplicate key mappings did not match.", 2,
					pair.getValue().size());
			assertEquals("The result of parsing the duplicate key mappings csv did not match.", refPair, pair);
			assertFalse("Parsing mappings containing a duplicate key did not print an error.",
					barr.toString().isEmpty());
			String correctErr = "Found duplicate entity id \"Key\". Skipping line." + System.lineSeparator();
			assertEquals(
					"The first line of the error log did not match what it should have been after parsing mappings with a duplicate key.",
					correctErr, barr.toString().substring(0, correctErr.length()));
		} finally {
			LogHandler.setError(oldErr);
		}
	}

	/**
	 * Makes sure that already existing values are ignored when they are read again
	 * in a mappings file.
	 * 
	 * @throws IOException if reading/writing/creating the temp file fails.
	 */
	@Test
	public void mappingsDuplicateValue() throws IOException {
		File inputFile = tempFolder.newFile("duplicate_value_mappings.csv");

		PrintStream oldErr = LogHandler.getError();
		try (PrintStream out = new PrintStream(inputFile);
				FileInputStreamHandler fiin = new FileInputStreamHandler(inputFile);
				ByteArrayOutputStream barr = new ByteArrayOutputStream()) {
			LogHandler.setError(new PrintStream(barr));
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
			assertFalse("Parsing mappings containing a duplicate value did not print an error.",
					barr.toString().isEmpty());
			String correctErr = "Found duplicate id \"Value 2\". Ignoring this occurrence." + System.lineSeparator();
			assertEquals(
					"The first line of the error log did was not as expected after parsing mappings with a duplicate value.",
					correctErr, barr.toString().substring(0, correctErr.length()));
		} finally {
			LogHandler.setError(oldErr);
		}
	}

}
