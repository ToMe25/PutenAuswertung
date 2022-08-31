package com.tome25.auswertung.tests.streamhandler.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.tests.rules.ErrorLogRule;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;

/**
 * A class containing unit tests related to {@link FileOutputStreamHandler}.
 * 
 * @author theodor
 */
public class FileOutputStreamHandlerTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Rule
	public ErrorLogRule errorLog = new ErrorLogRule();

	/**
	 * Checks whether writing a basic string to an {@link FileOutputStreamHandler}
	 * works as expected.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void writeBasicString() throws IOException {
		Pair<FileOutputStreamHandler, BufferedReader> tempFile = tempFolder.newTempOutputFile("basic_string_test.txt");
		FileOutputStreamHandler fiout = tempFile.getKey();
		BufferedReader bin = tempFile.getValue();

		fiout.println("Some Random Basic Test String");
		assertEquals("The first line of the temp file didn't match.", "Some Random Basic Test String", bin.readLine());
		assertNull("There was a second line in the temporary file.", bin.readLine());
	}

	/**
	 * Tests writing a whole bunch of lines to the output file.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void writeMany() throws IOException {
		Pair<FileOutputStreamHandler, BufferedReader> tempFile = tempFolder.newTempOutputFile("long_basic_test.txt");
		FileOutputStreamHandler fiout = tempFile.getKey();
		BufferedReader bin = tempFile.getValue();

		for (int i = 0; i < 100; i++) {
			fiout.println("Test line " + i);
			assertEquals("Some line of the of the output file didn't match.", "Test line " + i, bin.readLine());
		}
	}

	/**
	 * Test {@link FileOutputStreamHandler#println(String)} with a complex string
	 * containing a {@link System#lineSeparator()}.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void writeComplexMultilineString() throws IOException {
		Pair<FileOutputStreamHandler, BufferedReader> tempFile = tempFolder
				.newTempOutputFile("complex_multiline_test.txt");
		FileOutputStreamHandler fiout = tempFile.getKey();
		BufferedReader bin = tempFile.getValue();

		fiout.println("SKQ'asd4j0?,fha#\\vbaorjk21!$%612=/(´`>" + System.lineSeparator() + "|\"#%&/)?=)!§\\");
		assertEquals("The first line of the complex temp file didn't match.", "SKQ'asd4j0?,fha#\\vbaorjk21!$%612=/(´`>",
				bin.readLine());
		assertEquals("The second line of the complex temp file didn't match.", "|\"#%&/)?=)!§\\", bin.readLine());

		fiout.println("Some irrelevant string");
		assertEquals("The third line of the complex temp file didn't match.", "Some irrelevant string", bin.readLine());
	}

	/**
	 * Checks whether a default {@link FileOutputStreamHandler} ignores temporary
	 * data as expected.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void ignoreTemporary() throws IOException {
		File tempFile = tempFolder.newFile("ignore_temporary_test.txt");
		try (FileOutputStreamHandler fiout = new FileOutputStreamHandler(tempFile)) {
			fiout.println("Some temporary String", true);
			fiout.flush();
			assertEquals("The list of lines of the file after writing temporary content wasn't empty.", 0,
					Files.size(fiout.getOutputFile().toPath()));
			errorLog.checkLine(
					"Trying to write line \"Some temporary String\" to file output that does not handle temporary data.");
		}
	}

	/**
	 * Makes sure temporary output is printed by {@link FileOutputStreamHandler
	 * FileOutputStreamHandlers} with temporary output enabled.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void writeTemporary() throws IOException {
		File tempFile = tempFolder.newFile("write_temporary_test.txt");
		try (FileOutputStreamHandler fiout = new FileOutputStreamHandler(tempFile, true, true);
				BufferedReader bin = new BufferedReader(new FileReader(tempFile))) {
			fiout.println("Some temporary String", true);
			assertEquals("The output file did not contain the temporary line that was written.",
					"Some temporary String", bin.readLine());
		}
	}

	/**
	 * Makes sure that closing an already closed {@link FileOutputStreamHandler}
	 * only writes an error, and that writing to one doesn't actually write to the
	 * output file.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void writeClosed() throws IOException {
		Pair<FileOutputStreamHandler, BufferedReader> tempFile = tempFolder.newTempOutputFile("write_closed_test.txt");
		FileOutputStreamHandler fiout = tempFile.getKey();

		fiout.close();
		errorLog.checkEmpty();
		fiout.close();
		errorLog.checkLine("Trying to close an already closed FileOutputStreamHandler.", 0);
		errorLog.clear();

		fiout.println("Some test line");
		errorLog.checkLine("Tried to write line \"Some test line\" to an already closed FileOutputStreamHandler.", 0);
		assertEquals("Writing to a closed FileOutputStreamHandler caused the output file to have a size > 0.", 0,
				Files.size(fiout.getOutputFile().toPath()));
	}

	/**
	 * Tests printing day info of a {@link TurkeyInfo} object using a
	 * {@link FileOutputStreamHandler}.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void printDay() throws IOException {
		Pair<FileOutputStreamHandler, BufferedReader> tempFile = tempFolder.newTempOutputFile("print_day_test.csv");
		FileOutputStreamHandler fiout = tempFile.getKey();
		BufferedReader bin = tempFile.getValue();

		TurkeyInfo ti = new TurkeyInfo("0", Arrays.asList(new String[] { "T1", "T2", "T3" }), "Zone 1", "01:01:2022",
				10512, true);
		ti.changeZone("Zone 2", 20415, "01:01:2022");
		ti.changeZone("Zone 2", 100064, "01:01:2022");
		ti.changeZone("Zone 1", 599612, "01:01:2022");
		ti.changeZone("Zone 2", 43000000, "01:01:2022");
		ti.changeZone("Zone 1", 81512333, "01:01:2022");
		ti.endDay("01:01:2022");

		fiout.printDay(ti, "01:01:2022", Arrays.asList(new String[] { "Zone 1", "Zone 2", "Zone 3" }));
		assertEquals("The printed csv line for the given TurkeyInfo didn't match.",
				"0;01:01:2022;4;13:08:28.47;10:51:31.53;00:00:00.00", bin.readLine());
		assertNull("Printing a single day of a single TurkeyInfo produced multiple lines.", bin.readLine());
	}

}
