package com.tome25.auswertung.tests.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.tome25.auswertung.log.LogHandler;

/**
 * A class containing unit tests related to this projects {@link LogHandler}.
 * 
 * @author theodor
 */
public class LogHandlerTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	/**
	 * The original {@link System#out}.
	 */
	private static final PrintStream origOut = System.out;

	/**
	 * The original {@link System#err} from before all tests.
	 */
	private static final PrintStream origErr = System.err;

	/**
	 * Resets the {@link LogHandler} output streams as well as {@link System#out}
	 * and {@link System#err}.
	 */
	@After
	public void resetStreams() {
		LogHandler.setOutput(null);
		LogHandler.setError(null);
		LogHandler.resetSysOut();
		LogHandler.resetSysErr();
		System.setOut(origOut);
		System.setErr(origErr);
	}

	/**
	 * Confirm that {@link LogHandler#setOutput} changes where {@link LogHandler}
	 * writes to, but doesn't change {@link System#out}.
	 */
	@Test
	public void setOutput() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream pout = new PrintStream(bout);
		LogHandler.setOutput(pout);

		assertEquals("Setting the log handler output stream changed System.out.", origOut, System.out);
		assertEquals("The log handler output stream didn't match.", pout, LogHandler.getOutput());

		LogHandler.out_println("Test line");
		assertEquals("Writing to a custom output stream failed.", "Test line\n", bout.toString());
	}

	/**
	 * Confirm that {@link LogHandler#setError} changes where {@link LogHandler}
	 * writes to, but doesn't change {@link System#err}.
	 */
	@Test
	public void setError() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream pout = new PrintStream(bout);
		LogHandler.setError(pout);

		assertEquals("Setting the log handler error stream changed System.err.", origErr, System.err);
		assertEquals("The log handler error stream didn't match.", pout, LogHandler.getError());

		LogHandler.err_println("Test line");
		assertEquals("Writing to a custom output stream failed.", "Test line\n", bout.toString());
	}

	/**
	 * Makes sure that {@link LogHandler#out_println} without a set output stream
	 * writes to {@link System#out}.
	 */
	@Test
	public void writeOutput() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		System.setOut(new PrintStream(bout));
		LogHandler.out_println("Some test line");

		assertEquals("Writing to the log handler without a set output stream didn't write to System.out.",
				"Some test line\n", bout.toString());
	}

	/**
	 * Makes sure that {@link LogHandler#err_println} without a set error stream
	 * writes to {@link System#err}.
	 */
	@Test
	public void writeError() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		System.setErr(new PrintStream(bout));
		LogHandler.err_println("Some test line");

		assertEquals("Writing to the log handler without a set error stream didn't write to System.err.",
				"Some test line\n", bout.toString());
	}

	/**
	 * Tests overriding and resetting {@link System#out}.
	 */
	@Test
	public void overrideSysOut() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream pout = new PrintStream(bout);
		LogHandler.setOutput(pout);
		LogHandler.overrideSysOut();

		assertEquals("Overriding System.out didn't work.", pout, System.out);

		System.out.println("Another test line.");

		assertEquals("Writing to the new System.out didn't work.", "Another test line.\n", bout.toString());

		LogHandler.resetSysOut();

		assertEquals("Resetting the system output stream failed.", origOut, System.out);
	}

	/**
	 * Tests overriding and resetting {@link System#err}.
	 */
	@Test
	public void overrideSysErr() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream pout = new PrintStream(bout);
		LogHandler.setError(pout);
		LogHandler.overrideSysErr();

		assertEquals("Overriding System.err didn't work.", pout, System.err);

		System.err.println("Another test line.");

		assertEquals("Writing to the new System.err didn't work.", "Another test line.\n", bout.toString());

		LogHandler.resetSysErr();

		assertEquals("Resetting the system error stream failed.", origErr, System.err);
	}

	/**
	 * Tests adding a log file to the log handler output stream.
	 * 
	 * @throws IOException If creating or reading the log file failed.
	 */
	@Test
	public void addOutputLogFile() throws IOException {
		File tempFile = tempFolder.newFile("output.log");
		LogHandler.addLogFile(tempFile, true, false);
		BufferedReader logReader = new BufferedReader(new FileReader(tempFile));

		LogHandler.out_println("Output line");
		assertEquals("A message written to the log handler output didn't end up in the log file.", "Output line",
				logReader.readLine());
		assertFalse("The log file contained more than one line after writing one output line.", logReader.ready());

		LogHandler.err_println("Error line");
		assertFalse("The log file contained more content after writing to the error stream.", logReader.ready());
		logReader.close();
	}

	/**
	 * Tests adding a log file to the log handler error stream.
	 * 
	 * @throws IOException If creating or reading the log file failed.
	 */
	@Test
	public void addErrorLogFile() throws IOException {
		File tempFile = tempFolder.newFile("error.log");
		LogHandler.addLogFile(tempFile, false, true);
		BufferedReader logReader = new BufferedReader(new FileReader(tempFile));

		LogHandler.err_println("Error line");
		assertEquals("A message written to the log handler error didn't end up in the log file.", "Error line",
				logReader.readLine());
		assertFalse("The log file contained more than one line after writing one error line.", logReader.ready());

		LogHandler.out_println("Output line");
		assertFalse("The log file contained more content after writing to the output stream.", logReader.ready());
		logReader.close();
	}

	/**
	 * Tests adding a log file to the log handler output and error streams.
	 * 
	 * @throws IOException If creating or reading the log file failed.
	 */
	@Test
	public void addCombinedLogFile() throws IOException {
		File tempFile = tempFolder.newFile("combined.log");
		LogHandler.addLogFile(tempFile, true, true);
		BufferedReader logReader = new BufferedReader(new FileReader(tempFile));

		LogHandler.out_println("Output line");
		assertEquals("A message written to the log handler output didn't end up in the log file.", "Output line",
				logReader.readLine());
		assertFalse("The log file contained more than one line after writing one output line.", logReader.ready());

		LogHandler.err_println("Error line");
		assertEquals("A message written to the log handler error didn't end up in the log file.", "Error line",
				logReader.readLine());
		assertFalse("The log file contained more than one line after writing one error line.", logReader.ready());
		logReader.close();
	}

	/**
	 * Test adding a {@code null} log file to the log handler.
	 * 
	 * @throws NullPointerException  Always.
	 * @throws FileNotFoundException Idealy never.
	 */
	@Test(expected = NullPointerException.class)
	public void addNullLogFile() throws NullPointerException, FileNotFoundException {
		LogHandler.addLogFile(null, true, true);
	}

}
