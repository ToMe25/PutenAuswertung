package com.tome25.auswertung.tests.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.tome25.auswertung.LogHandler;
import com.tome25.auswertung.stream.MultiOutputStream;

/**
 * A {@link TestRule} that redirects the {@link LogHandler} error log to a
 * buffer to check its contents later.
 * 
 * @author theodor
 */
public class ErrorLogRule implements TestRule {

	/**
	 * The {@link ByteArrayOutputStream} to write the error log to.
	 */
	private final ByteArrayOutputStream bout = new ByteArrayOutputStream();

	/**
	 * The {@link PrintStream} to give to {@link LogHandler} to write to
	 * {@code bout}.
	 */
	private final PrintStream pout = new PrintStream(new MultiOutputStream(System.err, bout));

	/**
	 * The list containing all the so far found lines of error logging.
	 */
	private List<String> lines = new ArrayList<String>();

	@Override
	public Statement apply(Statement base, Description description) {
		return new ErrorLogStatement(base);
	}

	/**
	 * Makes sure that the given line was previously written to the log, and removes
	 * it so future calls for the same line will fail.<br/>
	 * This allows to check whether a line was written twice.
	 * 
	 * @param line The line to check the error log for. Case sensitive.
	 * @throws NullPointerException If {@code line} is {@code null}.
	 */
	public void checkLine(String line) throws NullPointerException {
		Objects.requireNonNull(line, "The line to check for cannot be null.");

		lines.addAll(Arrays.asList(bout.toString().split(System.lineSeparator())));
		bout.reset();

		boolean found = false;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).equals(line)) {
				found = true;
				lines.set(i, null);
				break;
			}
		}

		if (!found) {
			fail("Didn't find error log line \"" + line + "\".");
		}
	}

	/**
	 * Checks whether the error log line with the given number matches the given
	 * line.<br/>
	 * Then removes said line from the list of lines, so future calls of this and
	 * {@link ErrorLogRule#checkLine(String)} fail.
	 * 
	 * @param line The line to check the error log for. Case sensitive.
	 * @param nr   The line number of the log line to compare with the given line.
	 * @throws NullPointerException If {@code line} is {@code null}.
	 */
	public void checkLine(String line, int nr) throws NullPointerException {
		Objects.requireNonNull(line, "The line to check for can't be null.");

		checkNotEmpty();

		lines.addAll(Arrays.asList(bout.toString().split(System.lineSeparator())));
		bout.reset();

		String ln = lines.get(nr);
		lines.set(nr, null);

		assertEquals("The error log line number " + nr + " didn't match.", line, ln);
	}

	/**
	 * Confirms that the log is empty.<br/>
	 * 
	 * The log is considered empty when either
	 * <ul>
	 * <li>Nothing was written in it in the first place</li>
	 * <li>It was cleared using {@link ErrorLogRule#clear()}</li>
	 * <li>Every line was removed by calling {@link ErrorLogRule#checkLine(String)}
	 * or {@link ErrorLogRule#checkLine(String, int)}.</li>
	 * <ul>
	 */
	public void checkEmpty() {
		if (!logEmpty()) {
			fail("The error log wasn't empty.");
		}
	}

	/**
	 * Confirms that the error log is not empty.
	 * 
	 * The log is considered empty when either
	 * <ul>
	 * <li>Nothing was written in it in the first place</li>
	 * <li>It was cleared using {@link ErrorLogRule#clear()}</li>
	 * <li>Every line was removed by calling {@link ErrorLogRule#checkLine(String)}
	 * or {@link ErrorLogRule#checkLine(String, int)}.</li>
	 * <ul>
	 */
	public void checkNotEmpty() {
		if (logEmpty()) {
			fail("The error log was empty when it shouldn't be.");
		}
	}

	/**
	 * Removes all lines from the error log that were written before this call.
	 */
	public void clear() {
		lines.clear();
		bout.reset();
	}

	/**
	 * Checks whether there is any error logging record in this Rule.<br/>
	 * True if the internal {@link ByteArrayOutputStream} is empty and the lines
	 * list is empty or only contains {@code null}s.
	 * 
	 * @return Whether the this error log is empty.
	 */
	private boolean logEmpty() {
		if (bout.size() > 0) {
			return false;
		}

		for (String line : lines) {
			if (line != null && !line.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * A {@link Statement} subclass responsible for redirecting the error log to
	 * check it later.
	 * 
	 * @author theodor
	 */
	public class ErrorLogStatement extends Statement {

		/**
		 * The base statement to be executed before checking error lines.
		 */
		private final Statement base;

		public ErrorLogStatement(final Statement base) {
			this.base = base;
		}

		@Override
		public void evaluate() throws Throwable {
			PrintStream oldErr = LogHandler.getError();
			LogHandler.setError(pout);
			try {
				base.evaluate();
			} finally {
				LogHandler.setError(oldErr);
			}
		}

	}

}
