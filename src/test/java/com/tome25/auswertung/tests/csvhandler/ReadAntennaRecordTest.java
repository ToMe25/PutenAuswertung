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
	 * input file.
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
			AntennaRecord refRec = new AntennaRecord("Trans" + i * 2, "01.01.2022",
					String.format("%d:%d:%d.%d", i * 2, i * 3, i * 4, i * 8), "Ant" + i);
			assertEquals("One of the parsed antenna records didn't match.", refRec, rec);
			i++;
		}
		assertEquals("The number of parsed antenna records didn't match.", 12, i);
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
	 * Make sure that the {@link CSVHandler} does not read lines with too few
	 * columns.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readMissingColumn() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("missing_column_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;01.01.2022;Antenna1");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
		assertNull("An antenna record read with a missing column wasn't null.", rec);
		errorLog.checkLine(
				"Input line \"Trans1;01.01.2022;Antenna1\" did not contain exactly four tokens. Skipping line.", 0);
		errorLog.checkLine("Failed to read an antenna record from the input file.");
	}

	/**
	 * Makes sure that reading an {@link AntennaRecord} from a line with too many
	 * columns returns {@code null}.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readAdditionalColumn() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("additional_column_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;01.01.2022;08:58:29.61;Antenna1;Test");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
		assertNull("An antenna record read with an additional column wasn't null.", rec);
		errorLog.checkLine(
				"Input line \"Trans1;01.01.2022;08:58:29.61;Antenna1;Test\" did not contain exactly four tokens. Skipping line.",
				0);
		errorLog.checkLine("Failed to read an antenna record from the input file.");
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
	 * Tests handling of input lines with an empty token.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readEmptyColumn() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_column_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;;12:54:56.00;Ant1");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
		assertNull("An antenna record read with an empty column wasn't null.", rec);
		errorLog.checkLine("Input line \"Trans1;;12:54:56.00;Ant1\" contained an empty token. Skipping line.", 0);
		errorLog.checkLine("Failed to read an antenna record from the input file.");
	}

	/**
	 * Tests parsing input lines with an invalid time string.<br/>
	 * Tests both a time string without hours, and one that contains characters that
	 * aren't digits.
	 * 
	 * @throws IOException If reading/writing/creating the temp file fails.
	 */
	@Test
	public void readInvalidTime() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_time_data.csv");
		PrintStream pout = tempFile.getValue();
		FileInputStreamHandler fiin = tempFile.getKey();

		pout.println("Trans1;01.01.2022;15:24.12;Ant1");

		AntennaRecord rec = CSVHandler.readAntennaRecord(fiin);
		assertNull("An antenna record with an invalid time wasn't null.", rec);
		errorLog.checkLine("Parsing time of day of line \"Trans1;01.01.2022;15:24.12;Ant1\" failed. Skipping line.", 0);
		errorLog.checkLine("Failed to read an antenna record from the input file.");
		errorLog.clear();

		pout.println("Trans1;01.01.2020;312:12:Sec.12;Ant1");

		rec = CSVHandler.readAntennaRecord(fiin);
		assertNull("An antenna record with an invalid time wasn't null.", rec);
		errorLog.checkLine(
				"Parsing time of day of line \"Trans1;01.01.2020;312:12:Sec.12;Ant1\" failed. Skipping line.", 0);
		errorLog.checkLine("Failed to read an antenna record from the input file.");
	}

}
