package com.tome25.auswertung.log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * A class used to cache log messages until the arguments are parsed.<br/>
 * This is necessary because those can specify the log file.<br/>
 * Has to be registered as a {@link Runtime#addShutdownHook ShutdownHook} in
 * case of crashes.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class LogCacheShutdownHook implements Runnable {

	/**
	 * The {@link ByteArrayOutputStream} used as a stream cache.
	 */
	private final ByteArrayOutputStream cache = new ByteArrayOutputStream();

	/**
	 * The file to which to write the log cache when executed.
	 */
	private final File logFile;

	/**
	 * Creates a new LogCacheShutdowHook writing its cache to the given log file
	 * when executed.
	 * 
	 * @param logFile The file to write to.
	 * @throws NullPointerException If {@code logFile} is {@code null}.
	 */
	public LogCacheShutdownHook(File logFile) throws NullPointerException {
		Objects.requireNonNull(logFile, "The log file to write to on crash was null.");
		this.logFile = logFile.getAbsoluteFile();
	}

	/**
	 * Gets the {@link ByteArrayOutputStream} used as a log cache.<br/>
	 * Its content is written to the given log file when this {@link Runnable} is
	 * executed.
	 * 
	 * @return The {@link ByteArrayOutputStream} used as a log cache.<br/>
	 */
	public ByteArrayOutputStream getCacheStream() {
		return cache;
	}

	@Override
	public void run() {
		if (!logFile.exists()) {
			if (!logFile.getParentFile().exists()) {
				if (!logFile.getParentFile().mkdirs()) {
					LogHandler.err_println("Failed to create log file parent directory.");
					LogHandler.print_debug_info("Log File: \"%s\"", logFile);
				}
			}
		}

		try {
			FileOutputStream fiout = new FileOutputStream(logFile);
			fiout.write(cache.toByteArray());
			fiout.close();
			cache.close();
		} catch (IOException e) {
			LogHandler.err_println("Failed to write cache content to disk.");
			LogHandler.print_exception(e, "write cache to log file", "Log File: \"%s\", Cache Stream Content: \"%s\"",
					logFile, cache);
		}
	}

}
