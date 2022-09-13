package com.tome25.auswertung.tests.csvhandler;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.PrintStream;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.AntennaRecord;
import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.tests.rules.ErrorLogRule;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;

/**
 * A class containing unit tests related to the reading of {@link AntennaRecord
 * AntennaRecords} from antenna data input csvs.
 * 
 * @author theodor
 */
public class ReadAntennaRecordTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Rule
	public ErrorLogRule errorLog = new ErrorLogRule();

	/**
	 * Attempts to read a basic {@link AntennaRecord} from a data csv.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("basic_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;02.04.2021;22:01:25.32;Ant2");
		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("Trans1", "02.04.2021", "22:01:25.32", "Ant2");
		assertEquals("The read antenna record did not match.", refRec, rec);
	}

	/**
	 * Tests reading multiple {@link AntennaRecord AntennaRecords} from a single
	 * input file.<br/>
	 * Also makes sure reading after the last line returns {@code null}.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readMultiple() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("multiple_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		for (int i = 0; i < 12; i++) {
			pout.printf("Trans%d;01.01.2022;%d:%d:%d.%d;Ant%d%n", i * 2, i * 2, i * 3, i * 4, i * 8, i);
		}

		int i = 0;
		AntennaRecord rec = null;
		while ((rec = CSVHandler.readAntennaRecord(fiin, null)) != null) {
			if (i == 12) {
				assertNull("Reading another line after the last didn't return null.", rec); // Always fails.
			}

			AntennaRecord refRec = new AntennaRecord("Trans" + i * 2, "01.01.2022",
					String.format("%d:%d:%d.%d", i * 2, i * 3, i * 4, i * 8), "Ant" + i);
			assertEquals("One of the parsed antenna records didn't match.", refRec, rec);
			i++;
		}
		assertEquals("The number of parsed antenna records didn't match.", 12, i);

		// Make sure the last read caused an error log.
		errorLog.checkLine("Failed to read an antenna record from the input file.", 0);
	}

	/**
	 * Tests reading an {@link AntennaRecord} from a file with a header line.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readHeader() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("header_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Transponder;Date;Time;Antenna");
		pout.println("T5;01.03.2022;11:21:55.11;Ant7");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("T5", "01.03.2022", "11:21:55.11", "Ant7");
		assertEquals("The antenna record parsed after a header didn't match.", refRec, rec);
	}

	/**
	 * Attempts to read data in which the transponder and antenna id contain a
	 * space.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readSpaces() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("spaces_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Transponder 1;01.01.2022;01:23:45.67;Antenna 1");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("Transponder 1", "01.01.2022", "01:23:45.67", "Antenna 1");
		assertEquals("The antenna record parsed after a header didn't match.", refRec, rec);
	}

	/**
	 * Tests reading a line that ends with a value separator.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEndSeparator() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("end_separator_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Transponder 1;01.01.2022;01:23:45.67;Antenna 1;");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("Transponder 1", "01.01.2022", "01:23:45.67", "Antenna 1");
		assertEquals("The antenna record parsed after a header didn't match.", refRec, rec);
	}

	/**
	 * Make sure that the {@link CSVHandler} skips lines with too few columns.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readMissingColumn() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("missing_column_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;01.01.2022;Antenna1");
		pout.println("T2;01.01.2022;01:01:01.00;A2");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRef = new AntennaRecord("T2", "01.01.2022", "01:01:01.00", "A2");
		assertEquals("Reading a line with a missing column caused the next line to fail.", refRef, rec);
		errorLog.checkLine(
				"Input line \"Trans1;01.01.2022;Antenna1\" did not contain exactly four tokens. Skipping line.", 0);
	}

	/**
	 * Makes sure that lines with too many columns are skipped.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readAdditionalColumn() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("additional_column_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;01.01.2022;08:58:29.61;Antenna1;Test");
		pout.println("Trans1;01.01.2022;09:00:00.00;Antenna2");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("Trans1", "01.01.2022", "09:00:00.00", "Antenna2");
		assertEquals("The line after one with an additional column didn't match.", refRec, rec);
		errorLog.checkLine(
				"Input line \"Trans1;01.01.2022;08:58:29.61;Antenna1;Test\" did not contain exactly four tokens. Skipping line.",
				0);
	}

	/**
	 * Confirm that reading a {@code null} file throws a
	 * {@link NullPointerException}.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void readNull() throws NullPointerException {
		CSVHandler.readAntennaRecord(null, null);
	}

	/**
	 * Tests reading an empty input file.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEmptyFile() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_data.csv");
		FileInputStreamHandler fiin = tempFile.getKey();

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		assertNull("Reading from an empty file didn't return null.", rec);
		errorLog.checkLine("Failed to read an antenna record from the input file.", 0);
	}

	/**
	 * Tests that lines with empty columns are skipped, parsing the next line
	 * instead.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEmptyColumn() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_column_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;;12:54:56.00;Ant1");
		pout.println("Transponder2;01.01.2022;12:55:00.12;Antenna2");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("Transponder2", "01.01.2022", "12:55:00.12", "Antenna2");
		assertEquals("The antenna record after one with an empty column didn't match.", refRec, rec);
		errorLog.checkLine("Input line \"Trans1;;12:54:56.00;Ant1\" contained an empty token. Skipping line.", 0);
	}

	/**
	 * Tests parsing input lines with an invalid time string.<br/>
	 * Tests both a time string without hours, and one that contains characters that
	 * aren't digits.<br/>
	 * Makes sure that reading such a line doesn't cause the method to fail, but
	 * reads the next line.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidTime() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_time_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;01.01.2022;15:24.12;Ant1");
		pout.println("Trans2;02.05.2022;01:41:02.23;Ant3");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("Trans2", "02.05.2022", "01:41:02.23", "Ant3");
		assertEquals("Reading a line with invalid time didn't correctly read the next line.", refRec, rec);
		errorLog.checkLine(
				"Parsing time of day or date of line \"Trans1;01.01.2022;15:24.12;Ant1\" failed. Skipping line.", 0);

		pout.println("Trans1;01.01.2020;312:12:Sec.12;Ant1");
		pout.println("Transponder2;02.05.2022;01:41:02.23;Antenna2");

		rec = CSVHandler.readAntennaRecord(fiin, null);
		refRec = new AntennaRecord("Transponder2", "02.05.2022", "01:41:02.23", "Antenna2");
		assertEquals("The line after one with an invalid time didn't match.", refRec, rec);
		errorLog.checkLine(
				"Parsing time of day or date of line \"Trans1;01.01.2020;312:12:Sec.12;Ant1\" failed. Skipping line.");
	}

	/**
	 * Makes sure that the line after an empty one is parsed correctly.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEmptyLine() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_line_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println();
		pout.println("T1;01.01.2022;01:01:01.01;A1");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("T1", "01.01.2022", "01:01:01.01", "A1");
		assertEquals("The line after an empty one was read incorrectly.", refRec, rec);
		errorLog.checkLine("Skipped an empty line from input file.", 0);
	}

	/**
	 * Tests reading {@link AntennaRecord AntennaRecords} with reordered columns.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readReordered() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("reordered_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Transponder;Antenna;Time;Date");
		pout.println("Trans 1;Ant 2;05:21:56.12;13.05.2021");
		pout.println("Trans 1;Ant 1;12:12:12.12;13.05.2021");

		short order[] = new short[] { 0, 1, 2, 3 };
		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, order);
		AntennaRecord refRec = new AntennaRecord("Trans 1", "13.05.2021", "05:21:56.12", "Ant 2");
		assertEquals("The first read antenna record didn't match.", refRec, rec);
		assertArrayEquals("The token order array didn't match after reading the first token.",
				new short[] { 0, 3, 2, 1 }, order);
		rec = CSVHandler.readAntennaRecord(fiin, order);
		refRec = new AntennaRecord("Trans 1", "13.05.2021", "12:12:12.12", "Ant 1");
		assertEquals("The second read antenna record didn't match.", refRec, rec);
	}

	/**
	 * Test reading a header line containing an invalid header.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readUnknownHeader() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("unknown_header_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Transponder;Date;Test;Antenna");
		pout.println("T2;22.12.2012;01:02:03.04;A2");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("T2", "22.12.2012", "01:02:03.04", "A2");
		assertEquals("A default order antenna record wasn't read correctly.", refRec, rec);
		errorLog.checkLine("Found invalid header \"test\".", 0);
		errorLog.checkLine("Header line \"Transponder;Date;Test;Antenna\" was invalid. Assuming default column order.");
	}

	/**
	 * Tests reading a header line containing a duplicate header.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readDuplicateHeader() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("duplicate_header_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Transponder;Time;Time;Antenna");
		pout.println("T1;01.07.2022;12:54:27.93;A1");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("T1", "01.07.2022", "12:54:27.93", "A1");
		assertEquals("A default order antenna record wasn't read correctly.", refRec, rec);
		errorLog.checkLine("Header line \"Transponder;Time;Time;Antenna\" was invalid. Assuming default column order.",
				0);
	}

	/**
	 * Test giving {@link CSVHandler#readAntennaRecord} an invalid token order.
	 * 
	 * @throws IllegalArgumentException Always.
	 * @throws IOException              If creating the temp file fails.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void readInvalidOrder() throws IllegalArgumentException, IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_order_data.csv");
		FileInputStreamHandler fiin = tempFile.getKey();

		CSVHandler.readAntennaRecord(fiin, new short[] { 0, 1, 1, 2 });
	}

	/**
	 * Test giving {@link CSVHandler#readAntennaRecord} a token order with too few
	 * elements.
	 * 
	 * @throws IllegalArgumentException Always.
	 * @throws IOException              If creating the temp file fails.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void readInvalidOrderLen() throws IllegalArgumentException, IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_order_len_data.csv");
		FileInputStreamHandler fiin = tempFile.getKey();

		CSVHandler.readAntennaRecord(fiin, new short[] { 0, 1, 2 });
	}

	/**
	 * Tests reading a data file with a line with an invalid transponder.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidTransponder() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("invalid_transponder_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("#1;01.01.2022;05:53:17.71;Ant1");
		pout.println("Trans2;01.01.2022;17:41:32.59;Ant2");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("Trans2", "01.01.2022", "17:41:32.59", "Ant2");
		assertEquals("Reading a line with invalid transponder didn't correctly read the next line.", refRec, rec);
		errorLog.checkLine(
				"Input line \"#1;01.01.2022;05:53:17.71;Ant1\" contains invalid transponder id \"#1\". Skipping line.",
				0);
	}

	/**
	 * Tests reading a data file with a line with an invalid antenna.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidAntenna() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_antenna_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;01.01.2022;05:53:17.71;Antenna #1");
		pout.println("Trans2;01.01.2022;17:41:32.59;Ant2");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin, null);
		AntennaRecord refRec = new AntennaRecord("Trans2", "01.01.2022", "17:41:32.59", "Ant2");
		assertEquals("Reading a line with invalid transponder didn't correctly read the next line.", refRec, rec);
		errorLog.checkLine(
				"Input line \"Trans1;01.01.2022;05:53:17.71;Antenna #1\" contains invalid antenna id \"Antenna #1\". Skipping line.",
				0);
	}

}
