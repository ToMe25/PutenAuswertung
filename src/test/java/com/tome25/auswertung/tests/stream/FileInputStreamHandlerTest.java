package com.tome25.auswertung.tests.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;

/**
 * A class containing {@link FileInputStreamHandler} unit tests.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class FileInputStreamHandlerTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Rule
	public ExpectedException expectedExc = ExpectedException.none();

	/**
	 * A test for the most basic functionality of {@link FileInputStreamHandler}.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readBasicText() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("read_basic_text.txt");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fin = tempFile.getKey();

		assertFalse("Input stream handler available returned true on empty file.", fin.available());
		assertTrue("Input stream handler done returned false on empty file.", fin.done());
		out.println("A simple test string");
		assertTrue("Input stream handler available returned false on non empty file.", fin.available());
		assertFalse("Input stream handler done returned true on non empty file.", fin.done());
		assertEquals("The first line read from a simple file was not as expected.", "A simple test string",
				fin.readline());
		assertFalse("Input stream handler available returned true on fully read file.", fin.available());
		assertTrue("Input stream handler done returned false on fully read file.", fin.done());
	}

	/**
	 * Tests reading a 50 line input file in its entirety.
	 * 
	 * @throws IOException If reading/writing/creating the temporary file fails.
	 */
	@Test
	public void readLongText() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("read_longer_text.txt");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fin = tempFile.getKey();

		for (int i = 0; i < 50; i++) {
			out.println("Test line " + i);
		}

		out.close();

		int i = 0;
		while (fin.available()) {
			assertEquals("A read line did not match.", "Test line " + i, fin.readline());
			i++;
		}

		assertEquals("The number of lines read did not match expectations.", 50, i);
		assertFalse("Input stream handler available returned true on fully read file.", fin.available());
		assertTrue("Input stream handler done returned false on fully read file.", fin.done());
	}

	/**
	 * Checks {@code available} and {@code done} status of a
	 * {@link FileInputStreamHandler} when closed.<br/>
	 * Also verifies that {@code readline} throws when the handler is closed.
	 * 
	 * @throws IOException
	 */
	@Test
	public void readClosed() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("read_closed.txt");
		PrintStream out = tempFile.getValue();
		FileInputStreamHandler fin = tempFile.getKey();

		out.println("Some never read string.");
		assertTrue("Available is already false before closing.", fin.available());
		assertFalse("Done is true before closing before closing.", fin.done());
		fin.close();
		assertFalse("Available returned true on a closed handler.", fin.available());
		assertTrue("Done returned true on a file that has data, but is closed", fin.done());

		expectedExc.expect(IOException.class);
		fin.readline();
	}

	/**
	 * Makes sure that trying to read a file that doesn't exist throws an
	 * {@link IOException}.
	 * 
	 * @throws IOException always.
	 */
	@Test(expected = FileNotFoundException.class)
	public void readMissing() throws IOException {
		File tempFile = tempFolder.newFile("missing.txt");
		tempFile.delete();
		FileInputStreamHandler fin = new FileInputStreamHandler(tempFile);
		fin.close();// shouldn't ever by called.
	}

	/**
	 * Test creating a {@link FileInputStreamHandler} with a {@code null} input
	 * file.
	 * 
	 * @throws NullPointerException always.
	 * @throws IOException          if stuff goes wrong
	 */
	@Test(expected = NullPointerException.class)
	public void readNull() throws NullPointerException, IOException {
		FileInputStreamHandler fin = new FileInputStreamHandler(null);
		fin.close();// shouldn't ever by called.
	}

	/**
	 * Check reading a directory.
	 * 
	 * @throws IOException Always.
	 */
	@Test(expected = FileNotFoundException.class)
	public void readDirectory() throws IOException {
		File tempFile = tempFolder.newFolder("directory");
		FileInputStreamHandler fin = new FileInputStreamHandler(tempFile);
		fin.close();
	}

}
