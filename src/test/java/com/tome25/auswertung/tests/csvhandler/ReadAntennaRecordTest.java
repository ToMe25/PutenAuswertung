package com.tome25.auswertung.tests.csvhandler;

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
		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
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
		while ((rec = CSVHandler.readAntennaRecord(fiin)) != null) {
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
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("multiple_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Transponder;Date;Time;Antenna");
		pout.println("T5;01.03.2022;11:21:55.11;Ant7");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
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

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
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

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
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

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
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

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
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
		CSVHandler.readAntennaRecord(null);
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

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
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

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
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

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
		AntennaRecord refRec = new AntennaRecord("Trans2", "02.05.2022", "01:41:02.23", "Ant3");
		assertEquals("Reading a line with invalid time didn't correctly read the next line.", refRec, rec);
		errorLog.checkLine("Parsing time of day of line \"Trans1;01.01.2022;15:24.12;Ant1\" failed. Skipping line.", 0);

		pout.println("Trans1;01.01.2020;312:12:Sec.12;Ant1");
		pout.println("Transponder2;02.05.2022;01:41:02.23;Antenna2");

		rec = CSVHandler.readAntennaRecord(fiin);
		refRec = new AntennaRecord("Transponder2", "02.05.2022", "01:41:02.23", "Antenna2");
		assertEquals("The line after one with an invalid time didn't match.", refRec, rec);
		errorLog.checkLine(
				"Parsing time of day of line \"Trans1;01.01.2020;312:12:Sec.12;Ant1\" failed. Skipping line.");
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

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
		AntennaRecord refRec = new AntennaRecord("T1", "01.01.2022", "01:01:01.01", "A1");
		assertEquals("The line after an empty one was read incorrectly.", refRec, rec);
		errorLog.checkLine("Skipped an empty line from input file.", 0);
	}

}
