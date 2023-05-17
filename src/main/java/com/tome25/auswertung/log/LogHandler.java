package com.tome25.auswertung.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Objects;

import com.tome25.auswertung.stream.MultiOutputStream;
import com.tome25.auswertung.utils.Pair;

/**
 * The class responsible for handling the printing of status and debug info.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class LogHandler {

	/**
	 * Whether debug information should be written to the system output.
	 */
	private static volatile boolean debug = true;

	/**
	 * Whether anything, besides potentially the output data, should be written to
	 * the error and output stream.
	 */
	private static volatile boolean silent = false;

	/**
	 * The {@link PrintStream} to write system output messages to.<br/>
	 * Set to {@code null} to use {@link System#out}.
	 */
	private static PrintStream output = null;

	/**
	 * The {@link PrintStream} to write system error messages to.<br/>
	 * Set to {@code null} to use {@link System#err}.
	 */
	private static PrintStream error = null;

	/**
	 * The underlying {@link MultiOutputStream} writing the the primary output
	 * stream, and the log files.
	 */
	private static MultiOutputStream out;

	/**
	 * The underlying {@link MultiOutputStream} writing the the primary error
	 * stream, and the log files.
	 */
	private static MultiOutputStream err;

	/**
	 * The system output stream from before the LogHandler overrode it.
	 */
	private static PrintStream oldOut = null;

	/**
	 * The system error stream from before the LogHandler overrode it.
	 */
	private static PrintStream oldErr = null;

	/**
	 * The thread used as a {@link Runtime#addShutdownHook ShutdownHook} to write
	 * the log cache to disk in case of crashes.
	 */
	private static Thread shThread = null;

	/**
	 * The {@link Runtime#addShutdownHook ShutdownHook} writing the log cache
	 * contents to the disk if the program crashes.
	 */
	private static LogCacheShutdownHook cacheShutdownHook = null;

	/**
	 * A map containing the registered log files.<br/>
	 * The structure is {@code file -> stream, err, out}.<br/>
	 * {@code err} and {@code out} representing whether this file is currently being
	 * written to by error/output.
	 */
	private static Map<File, Pair<FileOutputStream, Pair<Boolean, Boolean>>> logFiles = new HashMap<File, Pair<FileOutputStream, Pair<Boolean, Boolean>>>();

	/**
	 * Initializes a log cache that is automatically written to the log file if the
	 * program crashes.
	 * 
	 * Overrides the default system output streams with a {@link MultiOutputStream}
	 * writing to the default output streams, and a log cache.<br/>
	 * And adds a {@link Runtime#addShutdownHook ShutdownHook} to write said cache
	 * to the default log file if the program crashes before a different log file is
	 * specified.
	 * 
	 * @param defaultLogFile The default log file to write to in case of a crash.
	 */
	public static void initLogCache(String defaultLogFile) {
		File logFile = new File(defaultLogFile);
		cacheShutdownHook = new LogCacheShutdownHook(logFile);
		shThread = new Thread(cacheShutdownHook);

		// Remove all previous syserr/sysout overrides
		resetSysErr();
		resetSysOut();

		// override syserr/sysout to write to the actual program output streams and the
		// cache
		System.setErr(
				new PrintStream(new MultiOutputStream(cacheShutdownHook.getCacheStream(), (oldErr = System.err))));
		System.setOut(
				new PrintStream(new MultiOutputStream(cacheShutdownHook.getCacheStream(), (oldOut = System.out))));

		// Write the cache to disk if the program crashes
		Runtime.getRuntime().addShutdownHook(shThread);

		// Make the LogHandler write to the current system streams
		setError(null);
		setOutput(null);
	}

	/**
	 * Disables the log cache, removes its shutdown hook, adds the new log files,
	 * and writes the log cache content to the new output log file.
	 * 
	 * @param newOutFile The file to write {@link System#out} messages to.
	 * @param newErrFile The file to write {@link System#err} messages to.
	 */
	public static void removeLogCache(File newOutFile, File newErrFile) {
		if (cacheShutdownHook == null) {
			err_println("Failed to remove log cache, since it wasn't enabled.");
			print_debug_info("Cache Hook: %s, shThread: %s, oldErr: %s, oldOut: %s", cacheShutdownHook, shThread,
					oldErr, oldOut);
			return;
		}

		if (oldErr == null || oldOut == null) {
			err_println("Coudn't write to original outptu streams, since they aren't stored.");
			print_debug_info("Cache Hook: %s, shThread: %s, oldErr: %s, oldOut: %s", cacheShutdownHook, shThread,
					oldErr, oldOut);
		}

		// Make the log handler only write to actual program output streams
		setError(oldErr);
		setOutput(oldOut);

		// Add the new log files
		if (newErrFile != null) {
			try {
				addLogFile(newErrFile, false, true);
			} catch (FileNotFoundException e) {
				err_println("Failed to open error log file. Error log will not be written to a file.");
				print_exception(e, "add log file", "Log file: \"%s\"", newErrFile.getAbsolutePath());
			}
		}

		if (newOutFile != null) {
			try {
				addLogFile(newOutFile, true, false);
			} catch (FileNotFoundException e) {
				err_println("Failed to open output log file. System output will nut be written to a file.");
				print_exception(e, "add log file", "Log file: \"%s\"", newOutFile.getAbsolutePath());
			}
		}

		// Write cache to new out file
		if (newOutFile != null) {
			try {
				FileOutputStream logFileStream = logFiles.get(newOutFile).getKey();
				logFileStream.write(cacheShutdownHook.getCacheStream().toByteArray());
			} catch (IOException e) {
				err_println("Failed to write cached log messages to new log file.");
				print_exception(e, "write cache to disk", "Log file: \"%s\"", newOutFile.getAbsolutePath());
			}
		}

		// Disable shutdown hook
		Runtime.getRuntime().removeShutdownHook(shThread);

		// Override system streams
		overrideSysErr();
		overrideSysOut();

		// Remove and close shutdown hook and its cache stream
		try {
			cacheShutdownHook.getCacheStream().close();
		} catch (IOException e) {
			err_println("Failed to close log cache.", true);
			print_exception(e, "close log cache stream", null);
		}
		cacheShutdownHook = null;
		shThread = null;
	}

	/**
	 * Writes the given string to the system output if not in silent mode.<br/>
	 * Changes to the next line after writing.<br/>
	 * The given text is not considered debug info, however it is not printed in
	 * silent mode.
	 * 
	 * @param line The string to write to the system output.
	 */
	public static void out_println(String line) {
		out_println(line, false);
	}

	/**
	 * Writes the given string to the system output if not in silent mode.<br/>
	 * Changes to the next line after writing.
	 * 
	 * @param line  The string to write to the system output.
	 * @param debug Whether the string should only be printed in debug mode.
	 */
	public static void out_println(String line, boolean debug) {
		if (debug && !LogHandler.debug) {
			return;
		}

		if (silent) {
			return;
		}

		if (output != null) {
			output.println(line);
		} else {
			System.out.println(line);
		}
	}

	/**
	 * Writes the given string to the system error stream if not in silent
	 * mode.<br/>
	 * Changes to the next line after writing.<br/>
	 * The given text is not considered debug info.
	 * 
	 * @param line The string to write to the system output.
	 */
	public static void err_println(String line) {
		err_println(line, false);
	}

	/**
	 * Writes the given string to the system error stream if not in silent
	 * mode.<br/>
	 * Changes to the next line after writing.
	 * 
	 * @param line  The string to write to the system output.
	 * @param debug Whether the string should only be printed in debug mode.
	 */
	public static void err_println(String line, boolean debug) {
		if (debug && !LogHandler.debug) {
			return;
		}

		if (silent) {
			return;
		}

		if (error != null) {
			error.println(line);
		} else {
			System.err.println(line);
		}
	}

	/**
	 * Prints the given debug information to the system error stream.<br/>
	 * Prints an error message if given an empty or {@code null} info arg.
	 * 
	 * @param info      The debug info string to print. Can contain formatting args.
	 * @param info_args The formatting args from info.
	 */
	public static void print_debug_info(String info, Object... info_args) {
		if (!debug || silent) {
			return;
		}

		if (info == null || info.isEmpty()) {
			LogHandler.err_println("print_debug_info: Received empty debug info string.", true);
			return;
		}

		String info_str = "Additional debug Information: " + String.format(info, info_args);
		err_println(info_str, true);
	}

	/**
	 * Prints the given exception and debug info to the system error stream.<br/>
	 * Does not print if not in debug mode.
	 * 
	 * @param ex        The exception to print.
	 * @param task      What was currently being done when the exception occurred.
	 * @param info      Additional debug information. Not printed at all if
	 *                  {@code null}.
	 * @param info_args String formatting args for info.
	 * @throws IllegalFormatException if the types or number of formatting args does
	 *                                not match those used in the info string.
	 */
	public static void print_exception(Exception ex, String task, String info, Object... info_args)
			throws IllegalFormatException {
		if (ex == null || !debug || silent) {
			return;
		}

		if (output != null) {
			output.flush();
		} else {
			System.out.flush();
		}

		if (task != null && !task.isEmpty()) {
			err_println("An Exception occurred while trying to " + task + '.');
		}

		if (info != null) {
			print_debug_info(info, info_args);
		}

		err_println("Exception stack trace:");
		if (error != null) {
			ex.printStackTrace(error);
			error.flush();
		} else {
			ex.printStackTrace(System.err);
			System.err.flush();
		}
	}

	/**
	 * Gets the current output stream the {@code LogHandler} writes log messages to.
	 * 
	 * @return The current standard output stream.
	 * @see #setOutput(PrintStream)
	 */
	public static PrintStream getOutput() {
		return output;
	}

	/**
	 * Sets the new output stream to write log messages to.<br/>
	 * Set to {@code null} to use {@link System#out}.<br/>
	 * Completely replaces the output stream, potentially removing all log files
	 * added using {@link #addLogFile}.
	 * 
	 * @param out the new standard output stream.
	 * @see #getOutput()
	 */
	public static synchronized void setOutput(PrintStream out) {
		output = out;
		LogHandler.out = null;

		for (File logFile : logFiles.keySet()) {
			Pair<Boolean, Boolean> used = logFiles.get(logFile).getValue();
			if (used.getValue()) {// log file is used for output
				if (!used.getKey()) {// log file isn't used for error
					logFiles.remove(logFile);
				} else {
					used.setValue(false);
				}
			}
		}
	}

	/**
	 * Gets the current output stream the {@code LogHandler} writes error messages
	 * to.
	 * 
	 * @return The current standard error stream.
	 * @see #setError(PrintStream)
	 */
	public static PrintStream getError() {
		return error;
	}

	/**
	 * Sets the new output stream to write error messages to.<br/>
	 * Set to {@code null} to use {@link System#err}.<br/>
	 * Completely replaces the error stream, potentially removing all log files
	 * added using {@link #addLogFile}.
	 * 
	 * @param err the new standard error stream.
	 * @see #getError()
	 */
	public static synchronized void setError(PrintStream err) {
		error = err;
		LogHandler.err = null;

		for (File logFile : logFiles.keySet()) {
			Pair<Boolean, Boolean> used = logFiles.get(logFile).getValue();
			if (used.getKey()) {// log file is used for error
				if (!used.getValue()) {// log file isn't used for output
					logFiles.remove(logFile);
				} else {
					logFiles.get(logFile).setValue(new Pair<Boolean, Boolean>(false, true));
				}
			}
		}
	}

	/**
	 * Checks whether the log handler is currently printing debug messages.
	 * 
	 * @return {@code true} if debug messages are printed.
	 */
	public static synchronized boolean isDebug() {
		return debug;
	}

	/**
	 * Sets whether the log handler should print debug messages.
	 * 
	 * @param debug Whether the log handler should print debug messages.
	 */
	public static synchronized void setDebug(boolean debug) {
		LogHandler.debug = debug;
	}

	/**
	 * Checks whether the log handler should print any messages at all.
	 * 
	 * @return {@code true} if the log handler does not print anything at all.
	 */
	public static synchronized boolean isSilent() {
		return silent;
	}

	/**
	 * Sets whether the log handler should be silent, meaning it doesn't print any
	 * messages at all.
	 * 
	 * @param silent Whether the log handler should be silent.
	 */
	public static synchronized void setSilent(boolean silent) {
		LogHandler.silent = silent;
	}

	/**
	 * Adds the given file as a log file to write the system log to.
	 * 
	 * @param log The file to write to.
	 * @param out If the file should be added as a log file for the output stream.
	 * @param err If the file should be added as a log file for the error stream.
	 * @throws FileNotFoundException if the file exists but is a directory rather
	 *                               than a regular file, does not exist but cannot
	 *                               be created, or cannot be opened for any other
	 *                               reason.
	 * @throws NullPointerException  If {@code log} is {@code null}.
	 * @see #removeLogFile(File, boolean, boolean)
	 */
	public static void addLogFile(File log, boolean out, boolean err)
			throws FileNotFoundException, NullPointerException {
		Objects.requireNonNull(log, "The log file to add can't be null.");

		if (!out && !err) {
			return;
		}

		if (!log.exists()) {
			if (!log.getAbsoluteFile().getParentFile().exists()) {
				if (!log.getAbsoluteFile().getParentFile().mkdirs()) {
					throw new FileNotFoundException("Failed to create parent directory.");
				}
			}
		}

		FileOutputStream fiout = null;
		boolean isErr = false;
		boolean isOut = false;

		if (logFiles.containsKey(log)) {
			fiout = logFiles.get(log).getKey();
			isErr = logFiles.get(log).getValue().getKey();
			isOut = logFiles.get(log).getValue().getValue();
		} else {
			fiout = new FileOutputStream(log);
		}

		if (err) {
			addErrorStream(fiout);
		}

		if (out) {
			addOutputStream(fiout);
		}

		if (logFiles.containsKey(log)) {
			logFiles.get(log).setValue(new Pair<Boolean, Boolean>(err || isErr, out || isOut));
		} else {
			logFiles.put(log,
					new Pair<FileOutputStream, Pair<Boolean, Boolean>>(fiout, new Pair<Boolean, Boolean>(err, out)));
		}
	}

	/**
	 * Removes the given log file from the list of log files to write to.<br/>
	 * Only works if it was set using {@link #addLogFile} and {@link #setOutput} for
	 * output or {@link #setError} for error wasn't called since then.
	 * 
	 * @param log The file to no longer log to.
	 * @param out Whether to remove the file from system output log, if possible.
	 * @param err Whether to remove the file from system error log, if possible.
	 * @throws NullPointerException If {@code log} is {@code null}.
	 * @see #addLogFile(File, boolean, boolean)
	 */
	public static void removeLogFile(File log, boolean out, boolean err) throws NullPointerException {
		Objects.requireNonNull(log, "The log file to remove can't be null.");

		if (!out && !err) {
			return;
		}

		FileOutputStream logOut = logFiles.get(log).getKey();

		if (err) {
			removeErrorStream(logOut);
			logFiles.get(log).setValue(new Pair<Boolean, Boolean>(false, logFiles.get(log).getValue().getValue()));
		}

		if (out) {
			removeOutputStream(logOut);
			logFiles.get(log).getValue().setValue(false);
		}

		if ((out && err) || (!logFiles.get(log).getValue().getKey() && !logFiles.get(log).getValue().getValue())) {
			logFiles.remove(log);
		}
	}

	/**
	 * Adds the given {@link OutputStream} as a output log target.
	 * 
	 * @param stream The stream to add.
	 * @return {@code true} if the stream was successfully added.
	 * @throws NullPointerException If {@code stream} was {@code null}.
	 */
	public static synchronized boolean addOutputStream(OutputStream stream) throws NullPointerException {
		Objects.requireNonNull(stream, "The new output stream cannot be null.");

		if (out == null) {
			out = new MultiOutputStream(output == null ? System.out : output);
			output = new PrintStream(out);
		}

		return out.addStream(stream);
	}

	/**
	 * Removes the given {@link OutputStream} from the output log targets, if it was
	 * used before.
	 * 
	 * @param stream The stream to remove.
	 * @return {@code true} if the stream was successfully removed.
	 * @throws NullPointerException If {@code stream} was {@code null}.
	 */
	public static boolean removeOutputStream(OutputStream stream) throws NullPointerException {
		Objects.requireNonNull(stream, "The output stream to remove cannot be null.");

		if (out == null) {
			return false;
		}

		return out.removeStream(stream);
	}

	/**
	 * Adds the given {@link OutputStream} as a error log target.
	 * 
	 * @param stream The stream to add.
	 * @return {@code true} if the stream was successfully added.
	 * @throws NullPointerException If {@code stream} was {@code null}.
	 */
	public static synchronized boolean addErrorStream(OutputStream stream) throws NullPointerException {
		Objects.requireNonNull(stream, "The new output stream cannot be null.");

		if (err == null) {
			err = new MultiOutputStream(error == null ? System.err : error);
			error = new PrintStream(err);
		}

		return err.addStream(stream);
	}

	/**
	 * Removes the given {@link OutputStream} from the error log targets, if it was
	 * used before.
	 * 
	 * @param stream The stream to remove.
	 * @return {@code true} if the stream was successfully removed.
	 * @throws NullPointerException If {@code stream} was {@code null}.
	 */
	public static boolean removeErrorStream(OutputStream stream) throws NullPointerException {
		Objects.requireNonNull(stream, "The output stream to remove cannot be null.");

		if (err == null) {
			return false;
		}

		return err.removeStream(stream);
	}

	/**
	 * Overrides the system standard output with the output stream used by this log
	 * handler.<br/>
	 * Doesn't do anything if this log handler is already writing to
	 * {@link System#out}.
	 * 
	 * @see #overrideSysErr()
	 * @see #resetSysOut()
	 * @see #getInitialSysOut()
	 */
	public static void overrideSysOut() {
		if (output == null) {
			return;
		}

		if (oldOut == null) {
			oldOut = System.out;
		}

		System.setOut(output);
	}

	/**
	 * Overrides the system standard error with the output stream used by this log
	 * handler.<br/>
	 * Doesn't do anything if this log handler is already writing errors to
	 * {@link System#err}.
	 * 
	 * @see #overrideSysOut()
	 * @see #resetSysErr()
	 * @see #getInitialSysErr()
	 */
	public static void overrideSysErr() {
		if (error == null) {
			return;
		}

		if (oldErr == null) {
			oldErr = System.err;
		}

		System.setErr(error);
	}

	/**
	 * Resets the system output stream to the one it was before it was changed by
	 * the log handler.<br/>
	 * If {@link #overrideSysOut()} was called twice, it resets to the one before
	 * the first call, unless this method was called between those two calls.
	 * 
	 * Does not do anything if {@link #overrideSysOut()} wasn't called since this
	 * method was last called.
	 * 
	 * @see #overrideSysOut()
	 * @see #resetSysErr()
	 * @see #getInitialSysOut()
	 */
	public static void resetSysOut() {
		if (oldOut != null) {
			System.setOut(oldOut);

			oldOut = null;
		}
	}

	/**
	 * Resets the system error stream to the one it was before it was changed by the
	 * log handler.<br/>
	 * If {@link #overrideSysErr()} was called twice, it resets to the one before
	 * the first call, unless this method was called between those two calls.
	 * 
	 * Does not do anything if {@link #overrideSysErr()} wasn't called since this
	 * method was last called.
	 * 
	 * @see #overrideSysErr()
	 * @see #resetSysOut()
	 * @see #getInitialSysErr()
	 */
	public static void resetSysErr() {
		if (oldErr != null) {
			System.setErr(oldErr);

			oldErr = null;
		}
	}

	/**
	 * Gets the initial system output stream, from before the handler overrode
	 * it.<br/>
	 * Or {@link System#out} if the LogHandler didn't override it.
	 * 
	 * @return The default system output stream.
	 * 
	 * @see #overrideSysOut()
	 * @see #resetSysOut()
	 * @see #getInitialSysErr()
	 */
	public static PrintStream getInitialSysOut() {
		if (oldOut == null) {
			return System.out;
		} else {
			return oldOut;
		}
	}

	/**
	 * Gets the initial system error stream, from before the handler overrode
	 * it.<br/>
	 * Or {@link System#err} if the LogHandler didn't override it.
	 * 
	 * @return The default system error stream.
	 * 
	 * @see #overrideSysErr()
	 * @see #resetSysErr()
	 * @see #getInitialSysOut()
	 */
	public static PrintStream getInitialSysErr() {
		if (oldErr == null) {
			return System.err;
		} else {
			return oldErr;
		}
	}
}
